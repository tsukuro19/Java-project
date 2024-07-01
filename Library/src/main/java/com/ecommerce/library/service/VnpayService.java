package com.ecommerce.library.service;

import com.ecommerce.library.model.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VnpayService {
    @Value("${vnp_TmnCode}")
    private String tmnCode;

    @Value("${vnp_HashSecret}")
    private String hashSecret;

    @Value("${vnp_Url}")
    private String vnpayUrl;

    // In-memory map to store transaction statuses
    private final Map<String, String> transactionStatus = new HashMap<>();

    public String generatePaymentUrl(Order order, String returnUrl) {
        // Generate VnPay payment URL
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        if(order.getTotalPrice()==0){
            vnpParams.put("vnp_Amount", String.valueOf(10000*100));
        }else{
            vnpParams.put("vnp_Amount", String.valueOf(order.getTotalPrice() * 100));
        }
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_BankCode","VNBANK");
        Random random = new Random();
        int vnp_TxnRef = 100000 + random.nextInt(900000);
        vnpParams.put("vnp_TxnRef", String.valueOf(vnp_TxnRef)); // Replace with your actual transaction reference
        vnpParams.put("vnp_OrderInfo", String.valueOf(order.getId())); // Replace with your actual order information

        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_TmnCode", tmnCode);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, 3); // Thêm 3 ngày
        String vnp_CreateDate = formatter.format(new Date());
        String vnp_ExpireDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_CreateDate", vnp_CreateDate);
        vnpParams.put("vnp_OrderType", "other");

        String secureHash = generateSecureHash(vnpParams, hashSecret,"SHA256");
        vnpParams.put("vnp_SecureHash", String.valueOf(secureHash)); // Replace with your actual secureHash);
        vnpParams.put("vnp_IpAddr", "192.168.1.5");

        // Store transaction status for later verification
        transactionStatus.put(String.valueOf(order.getId()), "pending");

        // Construct VnPay payment URL
        StringBuilder urlBuilder = new StringBuilder(vnpayUrl);
        urlBuilder.append('?');
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            urlBuilder.append(entry.getKey()).append('=').append(entry.getValue()).append('&');
        }
        urlBuilder.deleteCharAt(urlBuilder.length() - 1); // Remove last '&'
        System.out.println(urlBuilder.toString());
        return urlBuilder.toString();
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

    private String generateSecureHash(Map<String, String> params, String hashSecret, String hashType) {
        // Sort parameters alphabetically by key and concatenate them into a string
        String hashDataStr = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        // Append hashSecret to the end of hashDataStr
        hashDataStr += "&vnp_HashSecret=" + hashSecret;

        // Generate secure hash based on hashType
        String secureHash;
        try {
            MessageDigest digest = MessageDigest.getInstance(hashType);
            byte[] hash = digest.digest(hashDataStr.getBytes(StandardCharsets.UTF_8));

            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            secureHash = hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Handle exception
            e.printStackTrace();
            secureHash = null; // Or handle differently based on your application logic
        }

        return secureHash;
    }
}
