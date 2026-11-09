package bhoon.sugang_helper.planning.wishlist.repository;

import static bhoon.sugang_helper.planning.wishlist.entity.QWishlist.wishlist;

import bhoon.sugang_helper.academic.api.search.CourseWishlistQuery;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import org.springframework.stereotype.Component;

@Component
public class WishlistCourseSearch implements CourseWishlistQuery {

    @Override
    public BooleanExpression wishedBy(StringExpression courseKey, Long userId) {
        return JPAExpressions.selectOne().from(wishlist)
                .where(wishlist.userId.eq(userId), wishlist.courseKey.eq(courseKey))
                .exists();
    }

    @Override
    public NumberExpression<Long> popularity(StringExpression courseKey) {
        return Expressions.numberTemplate(Long.class, "({0})", JPAExpressions.select(wishlist.count())
                .from(wishlist).where(wishlist.courseKey.eq(courseKey)));
    }
}
