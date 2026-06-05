package com.example.bp.web.home;

import com.example.bp.support.AppProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/** XML sitemap of the public routes (FR-5). */
@Controller
public class SitemapController {

    private static final String[] PATHS = {"/", "/contact", "/terms", "/privacy"};

    private final String baseUrl;

    public SitemapController(AppProperties appProperties) {
        String url = appProperties.url();
        this.baseUrl = (url != null && url.endsWith("/")) ? url.substring(0, url.length() - 1) : url;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String path : PATHS) {
            String loc = "/".equals(path) ? baseUrl + "/" : baseUrl + path;
            xml.append("    <url>\n        <loc>").append(loc).append("</loc>\n    </url>\n");
        }
        xml.append("</urlset>\n");
        return xml.toString();
    }
}
