package com.agenthun.chaser.bean;

import com.agenthun.chaser.bean.base.BeidouNfcDevice;
import com.agenthun.chaser.bean.base.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * @project ESeal
 * @authors agenthun
 * @date 2016/12/16 04:37.
 */

public class BeidouNfcDeviceInfos {
    private List<Result> Result = new ArrayList<>();
    private List<BeidouNfcDevice> Details = new ArrayList<>();

    public List<com.agenthun.chaser.bean.base.Result> getResult() {
        return Result;
    }

    public void setResult(List<com.agenthun.chaser.bean.base.Result> result) {
        Result = result;
    }

    public List<BeidouNfcDevice> getDetails() {
        return Details;
    }

    public void setDetails(List<BeidouNfcDevice> details) {
        Details = details;
    }
}
