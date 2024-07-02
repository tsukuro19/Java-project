package com.ecommerce.library.service;

import com.ecommerce.library.model.Order;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class VnpayService {
    @Value("${vnp_TmnCode}")
    private String tmnCode;

    @Value("${vnp_HashSecret}")
    private String hashSecret;

    @Value("${vnp_Url}")
    private String vnpayUrl;

    // In-memory map to store transaction statuses
    private final Map<String, String> transactionStatus = new HashMap<>();

    public String generatePaymentUrl(Order order, String returnUrl,HttpServletRequest request) {
        // Generate VnPay payment URL
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        if(order.getTotalPrice()==0){
            vnpParams.put("vnp_Amount", String.valueOf(10000* 100L));
        }else{
            vnpParams.put("vnp_Amount", String.valueOf(order.getTotalPrice() * 100L));
        }
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_BankCode","VNBANK");
        vnpParams.put("vnp_IpAddr", getIpAddress(request));
        Random random = new Random();
        int vnp_TxnRef = 100000 + random.nextInt(900000);
        vnpParams.put("vnp_TxnRef", String.valueOf(vnp_TxnRef)); // Replace with your actual transaction reference
        vnpParams.put("vnp_OrderInfo", String.valueOf(order.getId())); // Replace with your actual order information
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_TmnCode", tmnCode);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        calendar.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_ExpireDate", vnp_ExpireDate);

        //build query url
        String queryUrl = getPaymentURL(vnpParams, true);
        String hashData = getPaymentURL(vnpParams, false);

        String secureHash = hmacSHA512(hashSecret,hashData);
        queryUrl += "&vnp_SecureHash=" + secureHash; // Replace with your actual secureHash);
        String paymentUrl = vnpayUrl + "?" + queryUrl;
        // Store transaction status for later verification
        transactionStatus.put(String.valueOf(order.getId()), "pending");

        // Construct VnPay payment URL
        StringBuilder urlBuilder = new StringBuilder(vnpayUrl);
        urlBuilder.append('?');
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            urlBuilder.append(entry.getKey()).append('=').append(entry.getValue()).append('&');
        }
        urlBuilder.deleteCharAt(urlBuilder.length() - 1); // Remove last '&'
        System.out.println(paymentUrl);
        return paymentUrl;
    }



    private Map<String, String> extractParamsFromResponse(String vnpResponse) {
        Map<String, String> responseParams = new HashMap<>();
        String[] params = vnpResponse.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                responseParams.put(keyValue[0], keyValue[1]);
            }
        }
        return responseParams;
    }

    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();

        } catch (Exception ex) {
            return "";
        }
    }
    public static String getIpAddress(HttpServletRequest request) {
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null) {
                ipAdress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            ipAdress = "Invalid IP:" + e.getMessage();
        }
        return ipAdress;
    }

    public static String getPaymentURL(Map<String, String> paramsMap, boolean encodeKey) {
        return paramsMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(entry ->
                        (encodeKey ? URLEncoder.encode(entry.getKey(),
                                StandardCharsets.US_ASCII)
                                : entry.getKey()) + "=" +
                                URLEncoder.encode(entry.getValue()
                                        , StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));
    }
}
