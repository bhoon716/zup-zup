/** 허용된 공개 계약으로만 협력하는 닫힌 모듈. */
@ApplicationModule(allowedDependencies = {"academic::operations", "common::api", "identity::notifications", "identity::users", "seatalert::api"})
package bhoon.sugang_helper.admin;

import org.springframework.modulith.ApplicationModule;
