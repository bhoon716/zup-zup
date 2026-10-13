package bhoon.sugang_helper.seatalert.notification.client;

import bhoon.sugang_helper.seatalert.enums.NotificationChannel;

public interface NotificationProviderExecutor {

    void execute(NotificationChannel provider, Runnable operation);
}
