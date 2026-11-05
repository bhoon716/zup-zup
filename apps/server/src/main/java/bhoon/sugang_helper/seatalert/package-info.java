/** 허용된 공개 계약으로만 협력하는 닫힌 모듈. */
@ApplicationModule(allowedDependencies = {"academic::catalog", "academic::events", "audit::api", "common::api", "identity::events", "identity::notifications", "identity::users"})
package bhoon.sugang_helper.seatalert;

import org.springframework.modulith.ApplicationModule;
