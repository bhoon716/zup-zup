/** 허용된 공개 계약으로만 협력하는 닫힌 모듈. */
@ApplicationModule(allowedDependencies = {"common::api", "identity::users"})
package bhoon.sugang_helper.academic;

import org.springframework.modulith.ApplicationModule;
