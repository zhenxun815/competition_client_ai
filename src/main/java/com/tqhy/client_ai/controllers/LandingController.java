package com.tqhy.client_ai.controllers;

import com.tqhy.client_ai.models.msg.BaseMsg;
import com.tqhy.client_ai.models.msg.local.LandingMsg;
import com.tqhy.client_ai.models.msg.local.VerifyMsg;
import com.tqhy.client_ai.network.Network;
import com.tqhy.client_ai.utils.NetworkUtils;
import com.tqhy.client_ai.utils.PropertyUtils;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Yiheng
 * @create 3/18/2019
 * @since 1.0.0
 */
@RestController
@Getter
@Setter
public class LandingController extends BaseWebviewController {

    static Logger logger = LoggerFactory.getLogger(LandingController.class);

    @FXML
    private WebView webView;

    @Value("${network.url.index:''}")
    private String indexUrl;

    @Value("${network.url.landing:''}")
    private String landingUrl;

    @Value("${network.url.test:''}")
    private String testUrl;

    @Value("${path.data:'/data/'}")
    private String localDataPath;

    private WebEngine webEngine;

    private boolean landingIgnore;


    public void jump2Index() {
        logger.info("load ai case ing index page..., {}", indexUrl);
        loadPage(webView, Network.SERVER_BASE_URL + indexUrl);
    }

    public void jump2Baidu() {
        logger.info("load ai case ing index page..., {}", indexUrl);
        loadPage(webView, Network.TEST_URL);
    }

    @FXML
    void initialize() {
        super.initialize(webView);
        loadPage(webView, Network.LOCAL_BASE_URL + landingUrl);
        /*String landingIgnoreConfig = PropertyUtils.getProperty(Constants.LANDING_IGNORE);

        landingIgnore = !StringUtils.isEmpty(landingIgnoreConfig) && Boolean.parseBoolean(landingIgnoreConfig);
        if (StringUtils.isEmpty(Network.SERVER_IP)) {
            logger.info("init load url is connection");
            loadConnectionPage();
        } else if (landingIgnore) {
            loadPage(webView, Network.SERVER_BASE_URL + "/case/release");
        } else {
            loadPage(webView, Network.LOCAL_BASE_URL + landingUrl);
        }*/

    }

    @PostMapping("/landing")
    @ResponseBody
    public VerifyMsg landing(@RequestBody LandingMsg landingMsg) {
        logger.info("get LandingMsg.." + landingMsg);
        VerifyMsg response = new VerifyMsg();

        String localIp = NetworkUtils.getLocalIp();
        if (!NetworkUtils.isIP(localIp)) {
            response.setFlag(BaseMsg.FAIL);
            response.setDesc("IP地址获取失败!");
            return response;
        }

        String userName = landingMsg.getUserName().trim();
        String userPwd = landingMsg.getUserPwd().trim();

        logger.info("request username: {}, password: {}", userName, userPwd);
        PropertyUtils.setUserName(userName);
       /* Network.getAicApi()
               .landing(userName, userPwd)
               .observeOn(Schedulers.io())
               .subscribeOn(Schedulers.trampoline())
               .map(body -> {
                   ClientMsg clientMsg = GsonUtils.parseResponseToObj(body);
                   logger.info("land response msg is {}", clientMsg);
                   logger.info("flag is: " + clientMsg.getFlag());
                   response.setFlag(clientMsg.getFlag());
                   response.setDesc(clientMsg.getDesc());
                   List<String> msg = clientMsg.getMsg();
                   String token = msg.get(0);
                   logger.info("map token is: " + token);
                   response.setToken(token);
                   response.setLocalIP(localIp);
                   response.setServerIP(Network.SERVER_IP);
                   logger.info("response server ip is: {}", Network.SERVER_IP);
                   return response;
               })
               .blockingSubscribe(res -> {
                   if (BaseMsg.SUCCESS == res.getFlag()) {
                       logger.info("subscribe token is {}", response.getToken());
                       Network.TOKEN = response.getToken();
                   }
               });*/
        response.setFlag(1);
        logger.info("response is {}", response);
        return response;
    }


    void sendMsgToJs(String funcName, String... msgs) {
        super.sendMsgToJs(webView, funcName, msgs);
    }
}
