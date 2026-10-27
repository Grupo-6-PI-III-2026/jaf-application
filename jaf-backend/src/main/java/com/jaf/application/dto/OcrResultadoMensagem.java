package com.jaf.application.dto;

import com.jaf.application.enums.StatusOcr;

public record OcrResultadoMensagem(String jobId, StatusOcr status, String textoBruto, Double confianca, String erro) {
}
