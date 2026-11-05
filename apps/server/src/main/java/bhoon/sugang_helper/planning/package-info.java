/** 허용된 공개 계약으로만 협력하는 닫힌 모듈. */
@ApplicationModule(allowedDependencies = {"academic::catalog", "academic::search", "common::api", "identity::events", "identity::users"})
package bhoon.sugang_helper.planning;

import org.springframework.modulith.ApplicationModule;
