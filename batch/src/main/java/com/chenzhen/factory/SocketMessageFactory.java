package com.chenzhen.factory;

import com.chenzhen.adapter.*;
import com.chenzhen.adapter.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SocketMessageFactory {

    @Autowired
    MMLoginAdapter mmLoginAdapter;

    @Autowired
    StartAdapter startAdapter;

    @Autowired
    CfcaInfoQryAdapter cfcaInfoQryAdapter;

    @Autowired
    UnBindAdapter unBindAdapter;

    @Autowired
    NGLoginAdapter nGLoginAdapter;

    public SocketMessageAdapter getInstall(String type) {
        switch (type) {
            case "1":
                return mmLoginAdapter;
            case "2":
                return startAdapter;
            case "3":
                return cfcaInfoQryAdapter;
            case "4":
                return unBindAdapter;
            case "5":
                return nGLoginAdapter;
            default:
             return null;
        }
    }
}
