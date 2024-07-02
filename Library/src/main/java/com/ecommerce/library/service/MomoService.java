package com.ecommerce.library.service;

import com.ecommerce.library.model.Order;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudinary.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class MomoService {
    private final String accessKey = "F8BBA842ECF85";
    private final String secretKey = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
    private final String partnerCode = "MOMO";
    private final String redirectUrl = "http://localhost:8020/shop/momo-return";
    private final String ipnUrl = "https://webhook.site/b3088a6a-2d17-4f8d-a383-71389a6c600b";
    private final String requestType = "payWithMethod";
    private final String orderInfo = "pay with MoMo";
    private final boolean autoCapture = true;
    private final String lang = "vi";

    public String generatePaymentUrl(Order order, String paymentCode) throws UnsupportedEncodingException {
        String orderId = partnerCode + System.currentTimeMillis();
        String requestId = orderId;
        String amount = String.valueOf(order.getTotalPrice());
        String extraData = ""; // pass empty value if your merchant does not have stores
        String orderGroupId = ""; // optional
        String rawOrderInfo = URLEncoder.encode(orderInfo, StandardCharsets.UTF_8.toString());
        String rawSignature = "accessKey=" + accessKey + "&amount=" + amount + "&extraData=" + extraData + "&ipnUrl=" + ipnUrl + "&orderId=" + orderId + "&orderInfo=" +rawOrderInfo + "&partnerCode=" + partnerCode + "&redirectUrl=" + redirectUrl + "&requestId=" + requestId + "&requestType=" + requestType;

        String signature = signHmacSHA256(rawSignature, secretKey);

        System.out.println(signature);

        // Prepare request body as a JSON object
        HashMap<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("partnerName", "Test");
        requestBody.put("storeId", "MomoTestStore");
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", redirectUrl);
        requestBody.put("ipnUrl", ipnUrl);
        requestBody.put("lang", lang);
        requestBody.put("requestType", requestType);
        requestBody.put("autoCapture", autoCapture);
        requestBody.put("extraData", extraData);
        requestBody.put("orderGroupId", orderGroupId);
        requestBody.put("signature", signature);
        requestBody.put("paymentCode", paymentCode);

        // Set headers and create HTTP entity
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<HashMap<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        System.out.println(headers);
        System.out.println(requestBody);
        System.out.println(requestEntity);
        // Make the POST request
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                "https://test-payment.momo.vn/v2/gateway/api/create",
                requestEntity,
                String.class);
        System.out.println(responseEntity);

        // Process the response
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode root = objectMapper.readTree(responseEntity.getBody());
                if (root != null && root.has("payUrl")) {
                    System.out.println(root.get("payUrl").asText());
                    return root.get("payUrl").asText();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse JSON response from MoMo API", e);
            }
        } else {
            throw new RuntimeException("Unexpected status code from MoMo API: " + responseEntity.getStatusCode());
        }

        return null; // If payUrl not found or any other issue
    }

    private String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Failed to encode value: " + value, e);
        }
    }

    private String signHmacSHA256(String data, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacData = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacData) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC SHA256 signature", e);
        }
    }
}
