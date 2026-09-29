package bhoon.sugang_helper.seatalert.notification.application.port;

import bhoon.sugang_helper.seatalert.contract.NotificationChannel;

public interface NotificationProviderExecutor {

    void execute(NotificationChannel provider, Runnable operation);
}
