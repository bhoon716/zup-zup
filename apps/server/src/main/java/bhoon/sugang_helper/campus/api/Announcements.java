package bhoon.sugang_helper.campus.api;

import java.util.List;

/** 다른 모듈이 사용하는 Announcements 계약. */
public interface Announcements {

    List<AnnouncementPreview> findDashboardAnnouncements(int limit);
}
