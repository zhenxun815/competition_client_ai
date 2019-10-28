package com.tqhy.client_ai.models.msg.local;

import com.tqhy.client_ai.models.msg.BaseMsg;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Yiheng
 * @create 3/22/2019
 * @since 1.0.0
 */
@Getter
@Setter
@ToString
public class LandingMsg extends BaseMsg {

    private String userName;

    private String userPwd;
}
