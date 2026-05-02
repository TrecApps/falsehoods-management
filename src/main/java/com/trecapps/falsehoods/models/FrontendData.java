package com.trecapps.falsehoods.models;

import com.trecauth.common.model.AccountList;
import lombok.Data;

@Data
public
class FrontendData<T> {
    AccountList accountList;
    T Data;
}
