package bhoon.sugang_helper.common.api.alerts;

/** 다른 모듈이 사용하는 OperationalAlerts 계약. */
public interface OperationalAlerts {

    void alert(SlackAlertCategory category, String errorCode, Throwable exception);

    void alert(SlackAlertCategory category, String errorCode, Throwable exception, String diagnostic);
}
