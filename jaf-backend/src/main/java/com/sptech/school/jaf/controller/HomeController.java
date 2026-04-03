package com.sptech.school.jaf.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return """
        <html>
        <head>
            <title>OCR - Nota Fiscal</title>
        </head>
        <body>
            <h2>Upload de Nota Fiscal</h2>

            <form method="POST"
                  action="/jaf/ocr/processar"
                  enctype="multipart/form-data">

                <input type="file" name="arquivo" accept="image/*" required />
                <br><br>

                <button type="submit">
                    Processar OCR
                </button>

            </form>

        </body>
        </html>
        """;
    }
}