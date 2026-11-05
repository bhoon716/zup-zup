/** 허용된 공개 계약으로만 협력하는 닫힌 모듈. */
@ApplicationModule(allowedDependencies = {"campus::api", "identity::users", "planning::api", "seatalert::api"})
package bhoon.sugang_helper.dashboard;

import org.springframework.modulith.ApplicationModule;
