package com.dazo66.controller;

import com.dazo66.config.PathConfig;
import com.dazo66.model.EverythingFileItem;
import com.dazo66.util.EverythingHtmlParser;
import com.dazo66.util.WebDavXmlUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.dazo66.util.EverythingHtmlParser.decodePath;
import static com.dazo66.util.EverythingHtmlParser.encodePath;

@Slf4j
@RestController
public class EverythingController {

    @Value("${everything.url:http://192.168.0.200:60004/}")
    private String everythingUrl;

    private volatile List<EverythingFileItem> rootCache;


    @Autowired
    private PathConfig pathConfig;


    @RequestMapping(value = "/**")
    public void handleWebDav(HttpServletRequest request, HttpServletResponse response,
                             @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) throws IOException {
        String method = request.getMethod().toUpperCase();
        String path = request.getRequestURI().substring(request.getContextPath().length());


        // Check Auth
        if (authHeader == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Everything\"");
            response.getWriter().write("Unauthorized: Please log in.");
            return;
        }
        if (rootCache == null && !checkAuth(authHeader, response)) {
            return;
        }

        String urlPath = path;
        path = decodePath(path);
        path = completeDrivePath(path);
        String realPath = getRealPath(path);

        log.info("Method: {}, UrlPath: {}, Path: {}, realPath: {}", method, urlPath, path, realPath);
        try {
            switch (method) {
                case "OPTIONS":
                    handleOptions(response);
                    break;
                case "PROPFIND":
                    handlePropfind(request, response, authHeader, path, realPath);
                    break;
                case "GET":
                    if (checkAuth(authHeader, response)) {
                        handleGet(request, response, realPath);
                    }
                    break;
                case "PUT":
                    if (checkAuth(authHeader, response)) {
                        handlePut(request, response, realPath);
                    }
                    break;
                case "DELETE":
                    if (checkAuth(authHeader, response)) {
                        handleDelete(response, realPath);
                    }
                    break;
                case "MKCOL":
                    handleMkcol(request, response, realPath);
                    break;
                case "MOVE":
                    if (checkAuth(authHeader, response)) {
                        handleMove(request, response, realPath);
                    }
                    break;
                case "COPY":
                    if (checkAuth(authHeader, response)) {
                        handleCopy(request, response, realPath);
                    }
                    break;
                case "LOCK":
                    handleLock(response, realPath);
                    break;
                case "UNLOCK":
                    handleUnlock(request, response, realPath);
                    break;
                default:
                    response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
                    break;
            }
        } catch (Exception e) {
            log.error("Error handling request", e);
            // response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
    }

    /**
     * 填充磁盘驱动器前缀
     * @param path
     * @return
     */
    private String completeDrivePath(String path) {
        if (rootCache != null && !path.equals("/")) {
            String rootPath = path.substring(1, path.indexOf("/", 1));
            for (EverythingFileItem fileItem : rootCache) {
                String cacheRootPath = decodePath(fileItem.getPath());
                String[] split = cacheRootPath.split("/");
                if (split.length >= 3 && split[2].equals(rootPath)) {
                    path = decodePath("/" + split[1] + path);
                }
            }
        }
        return path;
    }


    private String getRealPath(String requestPath) {
        // 1. URL Decode
        String path = requestPath;
        // 2. Remove leading slash if it looks like /C:/... or /D:/...
        // Typical WebDAV clients might send /C:/folder
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        // 3. Apply base.path.map replacement
        if (pathConfig.getMap() != null) {
            for (Map.Entry<String, String> entry : pathConfig.getMap().entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                // Simple prefix replacement
                if (path.startsWith(key)) {
                    // Replace the start
                    return val + path.substring(key.length());
                }
            }
        }
        return path;
    }

    private void handleOptions(HttpServletResponse response) {
        response.setStatus(HttpStatus.OK.value());
        response.setHeader("Allow", "OPTIONS, GET, HEAD, POST, DELETE, TRACE, PROPFIND, PROPPATCH, COPY, MOVE, LOCK, UNLOCK");
        response.setHeader("DAV", "1, 2");
    }

    private void handlePropfind(HttpServletRequest request, HttpServletResponse response, String authHeader, String path, String realPath) throws IOException {
        try {
            String requestPath = decodePath(request.getRequestURI().substring(request.getContextPath().length()));
            File file = new File(realPath);

            Connection.Response execute = Jsoup.connect(everythingUrl + path)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .ignoreContentType(true)
                    .method(Connection.Method.GET)
                    .maxBodySize(file.isFile() ? 1024 * 1024 * 2 : Integer.MAX_VALUE / 10)
                    .execute();

            String contentType = execute.header("Content-Type");
            List<EverythingFileItem> items;

            String currentHref = encodePath(requestPath);
            if (contentType != null && contentType.startsWith("text/html")) {
                items = EverythingHtmlParser.parse(execute.body());
                if (!StringUtils.hasLength(path) || "/".equals(path)) {
                    rootCache = items;
                }
                EverythingFileItem folderItem = new EverythingFileItem();
                String name = path;

                if (name.endsWith("/")) name = name.substring(0, name.length() - 1);
                int lastSlash = name.lastIndexOf('/');
                if (lastSlash >= 0) {
                    name = name.substring(lastSlash + 1);
                } else if (name.isEmpty()) {
                    name = "/"; // Root
                }
                folderItem.setName(name);
                folderItem.setPath(currentHref);
                folderItem.setIsFile(false);

                String depth = request.getHeader("Depth");
                if ("0".equals(depth)) {
                    items = Collections.singletonList(folderItem);
                } else {
                    // For Depth 1 (or default), we must include the collection itself + children
                    // Ensure list is modifiable
                    if (items.isEmpty() || !(items instanceof java.util.ArrayList)) {
                        items = new java.util.ArrayList<>(items);
                    }
                    items.add(0, folderItem);
                }
            } else {
                EverythingFileItem fileItem = new EverythingFileItem();
                fileItem.setPath(currentHref);
                fileItem.setName(file.getName());
                fileItem.setIsFile(true);
                String contentLength = execute.header("Content-Length");
                if (contentLength != null) {
                    fileItem.setSize(Double.parseDouble(contentLength) / 1024.0);
                }
                items = Collections.singletonList(fileItem);
            }

            String webDavXml = WebDavXmlUtil.toWebDavXml(items);
            response.setStatus(207); // Multi-Status
            response.setCharacterEncoding("UTF-8");
            response.setContentType(MediaType.TEXT_XML_VALUE);
            response.getWriter().write(webDavXml);

        } catch (HttpStatusException e) {
            log.error("handlePropfind HttpStatusException", e);
            if (e.getStatusCode() == 401) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Everything\"");
            } else {
                response.sendError(e.getStatusCode(), e.getMessage());
            }
        }
    }

    private void handleGet(HttpServletRequest request, HttpServletResponse response, String realPath) throws IOException {
        downloadFile(request, response, realPath);
    }

    private void downloadFile(HttpServletRequest request, HttpServletResponse response, String realPath) throws IOException {
        File file = new File(realPath);
        if (!file.exists()) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "File not found");
            return;
        }
        if (file.isDirectory()) {
            // Should verify if it reached here.
            // GET on directory usually handled by handleGet logic above (Everything proxy).
            // But if we fall through here:
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Cannot download a directory");
            return;
        }

        long fileLength = file.length();
        String rangeHeader = request.getHeader("Range");

        // Set content type
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        try {
            String probeType = Files.probeContentType(file.toPath());
            if (probeType != null) {
                contentType = probeType;
            }
        } catch (IOException ignored) {
        }
        response.setContentType(contentType);

        // Support Accept-Ranges
        response.setHeader("Accept-Ranges", "bytes");
        // ETag / Last-Modified
        response.setDateHeader("Last-Modified", file.lastModified());
        String etag = "\"" + fileLength + "-" + file.lastModified() + "\"";
        response.setHeader("ETag", etag);

        if (rangeHeader == null) {
            // Full content
            response.setStatus(HttpStatus.OK.value());
            response.setContentLengthLong(fileLength);
            try (InputStream in = new FileInputStream(file);
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } else {
            // Partial content
            long start = 0;
            long end = fileLength - 1;

            try {
                if (rangeHeader.startsWith("bytes=")) {
                    String[] ranges = rangeHeader.substring(6).split("-");
                    if (ranges.length > 0 && !ranges[0].isEmpty()) {
                        start = Long.parseLong(ranges[0]);
                    }
                    if (ranges.length > 1 && !ranges[1].isEmpty()) {
                        end = Long.parseLong(ranges[1]);
                    }
                }
            } catch (NumberFormatException e) {
                // Ignore invalid range, return full content? Or 416?
                // Fallback to full content usually safer or ignore invalid part
            }

            if (start > end || start >= fileLength) {
                response.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
                response.setHeader("Content-Range", "bytes */" + fileLength);
                return;
            }

            if (end >= fileLength) {
                end = fileLength - 1;
            }

            long contentLength = end - start + 1;
            response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
            response.setContentLengthLong(contentLength);
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);

            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r");
                 OutputStream out = response.getOutputStream()) {
                raf.seek(start);
                byte[] buffer = new byte[8192];
                long bytesToRead = contentLength;
                while (bytesToRead > 0) {
                    int readSize = (int) Math.min(buffer.length, bytesToRead);
                    int bytesRead = raf.read(buffer, 0, readSize);
                    if (bytesRead == -1) {
                        break;
                    }
                    out.write(buffer, 0, bytesRead);
                    bytesToRead -= bytesRead;
                }
            } catch (IOException e) {
                log.debug("Client aborted download: {}", e.getMessage());
            }
        }
        log.info("Downloaded file: {} (Range: {})", realPath, rangeHeader);
    }

    @SneakyThrows
    private boolean checkAuth(String authHeader, HttpServletResponse response) {
        try {
            // 尝试连接一下 用于校验权限 无权限不可下载
            Connection.Response execute = Jsoup.connect(everythingUrl)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .ignoreContentType(true)
                    .method(Connection.Method.GET)
                    .maxBodySize(1024000)
                    .execute();


            rootCache = EverythingHtmlParser.parse(execute.body());


            return true;
        } catch (HttpStatusException e) {
            if (e.getStatusCode() == 401 || e.getStatusCode() == 403) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Everything\"");
            }
            return false;
        }

    }

    private void handlePut(HttpServletRequest request, HttpServletResponse response, String realPath) throws IOException {


        File file = new File(realPath);
        // Ensure parent directories exist? 
        // WebDAV spec says PUT on non-existing parent should fail with 409 Conflict usually, 
        // but creating them is friendlier. Let's stick to standard: parent must exist.
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            response.sendError(HttpStatus.CONFLICT.value(), "Parent directory does not exist");
            return;
        }

        try (InputStream in = request.getInputStream();
             OutputStream out = Files.newOutputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        log.info("PUT file: {}", realPath);
        response.setStatus(HttpStatus.CREATED.value());
    }

    private void handleDelete(HttpServletResponse response, String realPath) throws IOException {
        File file = new File(realPath);
        if (!file.exists()) {
            response.sendError(HttpStatus.NOT_FOUND.value());
            return;
        }

        // Recursive delete if directory?
        // WebDAV DELETE on collection must delete all members.
        if (file.isDirectory()) {
            deleteRecursive(file);
        } else {
            if (!file.delete()) {
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to delete file");
                return;
            }
        }
        log.info("DELETE file: {}", realPath);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    private void deleteRecursive(File file) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        if (!file.delete()) {
            throw new IOException("Failed to delete " + file.getAbsolutePath());
        }
    }

    private void handleMkcol(HttpServletRequest request, HttpServletResponse response, String realPath) throws IOException {
        File file = new File(realPath);
        if (file.exists()) {
            response.sendError(HttpStatus.METHOD_NOT_ALLOWED.value(), "Resource already exists");
            return;
        }
        // Check body. MKCOL with body is unsupported.
        if (request.getContentLength() > 0) {
            response.sendError(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
            return;
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            response.sendError(HttpStatus.CONFLICT.value(), "Parent directory does not exist");
            return;
        }

        if (file.mkdir()) {
            log.info("MKCOL: {}", realPath);
            response.setStatus(HttpStatus.CREATED.value());
        } else {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to create directory");
        }
    }

    private void handleMove(HttpServletRequest request, HttpServletResponse response, String realPath) throws IOException {
        String destHeader = request.getHeader("Destination");
        if (destHeader == null) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Destination header missing");
            return;
        }

        String destRealPath = getDestRealPath(request, destHeader);
        if (destRealPath == null) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid Destination");
            return;
        }

        File src = new File(realPath);
        File dest = new File(destRealPath);

        if (!src.exists()) {
            response.sendError(HttpStatus.NOT_FOUND.value());
            return;
        }

        boolean overwrite = "T".equalsIgnoreCase(request.getHeader("Overwrite"));
        if (dest.exists() && !overwrite) {
            response.sendError(HttpStatus.PRECONDITION_FAILED.value(), "Destination exists and Overwrite is F");
            return;
        }

        // Move
        try {
            // Ensure parent of dest exists
            File destParent = dest.getParentFile();
            if (destParent != null && !destParent.exists()) {
                response.sendError(HttpStatus.CONFLICT.value(), "Destination parent does not exist");
                return;
            }

            Files.move(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("MOVE from {} to {}", realPath, destRealPath);
            response.setStatus(dest.exists() ? HttpStatus.NO_CONTENT.value() : HttpStatus.CREATED.value());

        } catch (IOException e) {
            log.error("Move failed", e);
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
    }

    private void handleCopy(HttpServletRequest request, HttpServletResponse response, String realPath) throws IOException {
        String destHeader = request.getHeader("Destination");
        if (destHeader == null) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Destination header missing");
            return;
        }

        String destRealPath = getDestRealPath(request, destHeader);
        if (destRealPath == null) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid Destination");
            return;
        }

        File src = new File(realPath);
        File dest = new File(destRealPath);

        if (!src.exists()) {
            response.sendError(HttpStatus.NOT_FOUND.value());
            return;
        }

        boolean overwrite = "T".equalsIgnoreCase(request.getHeader("Overwrite"));
        if (dest.exists() && !overwrite) {
            response.sendError(HttpStatus.PRECONDITION_FAILED.value(), "Destination exists and Overwrite is F");
            return;
        }

        try {
            // Copy recursively?
            copyRecursive(src.toPath(), dest.toPath(), overwrite);
            log.info("COPY from {} to {}", realPath, destRealPath);
            response.setStatus(dest.exists() ? HttpStatus.NO_CONTENT.value() : HttpStatus.CREATED.value());
        } catch (IOException e) {
            log.error("Copy failed", e);
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
    }

    private void copyRecursive(Path src, Path dest, boolean overwrite) throws IOException {
        if (Files.isDirectory(src)) {
            if (!Files.exists(dest)) {
                Files.createDirectories(dest);
            }
            try (java.util.stream.Stream<Path> stream = Files.list(src)) {
                for (Path child : (Iterable<Path>) stream::iterator) {
                    copyRecursive(child, dest.resolve(src.relativize(child)), overwrite);
                }
            }
        } else {
            if (overwrite) {
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            } else if (!Files.exists(dest)) {
                Files.copy(src, dest);
            }
        }
    }

    private String getDestRealPath(HttpServletRequest request, String destHeader) throws IOException {
        // Destination is a full URI: http://host:port/path/to/dest
        // We need to extract the path part relative to our app context
        try {
            URI destUri = new URI(destHeader);
            String rawPath = destUri.getRawPath(); // /path/to/dest
            // Remove context path if present
            String contextPath = request.getContextPath();
            if (rawPath.startsWith(contextPath)) {
                rawPath = rawPath.substring(contextPath.length());
            }
            return getRealPath(rawPath);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private void handleLock(HttpServletResponse response, String path) {
        // Mock LOCK
        log.info("LOCK: {}", path);
        response.setStatus(HttpStatus.OK.value());
        // Return dummy lock token XML
        String lockXml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                "<D:prop xmlns:D=\"DAV:\">\n" +
                "  <D:lockdiscovery>\n" +
                "    <D:activelock>\n" +
                "      <D:locktype><D:write/></D:locktype>\n" +
                "      <D:lockscope><D:exclusive/></D:lockscope>\n" +
                "      <D:depth>Infinity</D:depth>\n" +
                "      <D:owner>\n" +
                "        <D:href>client</D:href>\n" +
                "      </D:owner>\n" +
                "      <D:timeout>Second-3600</D:timeout>\n" +
                "      <D:locktoken>\n" +
                "        <D:href>opaquelocktoken:dummy-token</D:href>\n" +
                "      </D:locktoken>\n" +
                "    </D:activelock>\n" +
                "  </D:lockdiscovery>\n" +
                "</D:prop>";
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.TEXT_XML_VALUE);
        try {
            response.getWriter().write(lockXml);
        } catch (IOException e) {
            // ignore
        }
    }

    private void handleUnlock(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
        log.info("UNLOCK: {}", path);
        String lockToken = request.getHeader("Lock-Token");
        if (lockToken == null) {
            // 虽然是 Mock，但检查一下头部比较符合规范
            // 有些客户端可能不传？暂且允许，或者记录一下
            log.warn("UNLOCK request missing Lock-Token header for path: {}", path);
        }

        // 204 No Content 是标准的成功响应
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }
}
