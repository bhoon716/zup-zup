package bhoon.sugang_helper.campus.announcement.contract;

import java.util.List;

public interface DashboardAnnouncementQuery {

    List<AnnouncementPreview> findDashboardAnnouncements(int limit);
}
