package bhoon.sugang_helper.campus.api;

import java.util.List;

public interface Announcements {

    List<AnnouncementPreview> findDashboardAnnouncements(int limit);
}
