package bhoon.sugang_helper.common.api.alerts;

public interface OperationalAlerts {

    void alert(SlackAlertCategory category, String errorCode, Throwable exception);

    void alert(SlackAlertCategory category, String errorCode, Throwable exception, String diagnostic);
}
