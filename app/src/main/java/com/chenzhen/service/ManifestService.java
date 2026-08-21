package com.chenzhen.service;

import java.util.List;

public interface ManifestService {

    List<String> getOrderList(String orderO);

    List<String> datadict(String orderO);

    List<String> requestData(String orderO);

    List<String> responseData(String orderO);
}
