package com.chenahua.transform.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@ToString
@Getter
@Setter
@Accessors(chain = true)
public class MarketQuoteVO {
    String curveName;
    String instrumentType;
    String instrumentName;
    String tenor;
    String quote;
    String maturityDate;
    String mHRepDate;
}
