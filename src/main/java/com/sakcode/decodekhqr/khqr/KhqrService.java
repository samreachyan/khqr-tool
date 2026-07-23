package com.sakcode.decodekhqr.khqr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sakcode.decodekhqr.util.BakongUtils;
import kh.gov.nbc.bakong_khqr.BakongKHQR;
import kh.gov.nbc.bakong_khqr.model.CRCValidation;
import kh.gov.nbc.bakong_khqr.model.IndividualInfo;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRDecodeData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import kh.gov.nbc.bakong_khqr.model.MerchantInfo;

/**
 * Thin business-logic facade over {@link BakongUtils}/{@link BakongKHQR}. Depends only on SDK
 * types (no JavaFX), so it can be exercised without a UI.
 */
public final class KhqrService {

    public record GenerationOutcome(boolean success, String qrCode, String json, String errorMessage) {
    }

    public record DecodeOutcome(boolean valid, KHQRDecodeData data, String json, String errorMessage) {
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public GenerationOutcome generateIndividual(IndividualInfo individualInfo, String merchantCategoryCode) {
        return toOutcome(BakongUtils.generateIndividual(individualInfo, merchantCategoryCode));
    }

    public GenerationOutcome generateMerchant(MerchantInfo merchantInfo, String merchantCategoryCode) {
        return toOutcome(BakongUtils.generateMerchant(merchantInfo, merchantCategoryCode));
    }

    public DecodeOutcome decodeAndVerify(String qrCode) {
        KHQRResponse<KHQRDecodeData> decoded = BakongKHQR.decode(qrCode);
        KHQRResponse<CRCValidation> verification = BakongKHQR.verify(qrCode);

        String json = toPrettyJson(decoded);
        boolean valid = verification.getKHQRStatus().getCode() == 0;
        KHQRDecodeData data = decoded.getKHQRStatus().getCode() == 0 ? decoded.getData() : null;

        return new DecodeOutcome(valid, data, json, verification.getKHQRStatus().getMessage());
    }

    private GenerationOutcome toOutcome(KHQRResponse<KHQRData> response) {
        String json = toPrettyJson(response);
        boolean success = response.getKHQRStatus().getCode() == 0;
        String qrCode = success ? response.getData().getQr() : null;
        String errorMessage = success ? null : response.getKHQRStatus().getMessage();
        return new GenerationOutcome(success, qrCode, json, errorMessage);
    }

    private String toPrettyJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "Error serializing JSON: " + e.getMessage();
        }
    }
}
