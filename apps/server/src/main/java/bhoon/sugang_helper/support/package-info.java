/** 허용된 공개 계약으로만 협력하는 닫힌 모듈. */
@ApplicationModule(allowedDependencies = {"audit::api", "common::api", "identity::events", "identity::users"})
package bhoon.sugang_helper.support;

import org.springframework.modulith.ApplicationModule;
