package org.search.search.controller;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
// ↑↑↑↑↑↑  修改在这里！红色的错误应该会立刻消失 ↑↑↑↑↑↑

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/reading")
public class ReadingRedirectController {

    @Value("${reading.app.domain:http://localhost:10000/edit.html}")
    private String readingDomain;

    @GetMapping("/redirect")
    public ResponseEntity<Map<String, Object>> redirectToReading(
            @RequestParam Integer bookId,
            @RequestParam Integer chapterId,
            @RequestParam(required = false) Integer paragraphOrder, // 修改为 paragraphOrder
            HttpServletRequest request
    ) {
        String token = Optional.ofNullable(request.getCookies())
                .flatMap(cookies -> Arrays.stream(cookies)
                        .filter(cookie -> "jwtToken".equals(cookie.getName()))
                        .findFirst()
                        .map(Cookie::getValue))
                .orElse(null);

        UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromHttpUrl(readingDomain)
                .queryParam("bookId", bookId)
                .queryParam("chapterId", chapterId);

        if (paragraphOrder != null) {
            urlBuilder.queryParam("paragraphOrder", paragraphOrder);
        }

        if (token != null && !token.isEmpty()) {
            urlBuilder.queryParam("token", token);
        }

        String finalUrl = urlBuilder.toUriString();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "url", finalUrl
        ));
    }
}
