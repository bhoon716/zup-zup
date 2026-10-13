package bhoon.sugang_helper.campus.announcement.service;

import bhoon.sugang_helper.campus.announcement.dto.AnnouncementPreview;
import java.util.List;

public interface DashboardAnnouncementQuery {

    List<AnnouncementPreview> findDashboardAnnouncements(int limit);
}
