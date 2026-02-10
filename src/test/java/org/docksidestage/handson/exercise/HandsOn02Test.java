package org.docksidestage.handson.exercise;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.unit.UnitContainerTestCase;

/**
 * @author a.kumoshita
 */
public class HandsOn02Test extends UnitContainerTestCase {

    @Resource
    private MemberBhv memberBhv;

    public void test_existsTestData() throws Exception {
        // ## Arrange ##

        // ## Act ##
        int count = memberBhv.selectCount(cb -> {});

        // ## Assert ##
        assertTrue(count > 0);
    }

    public void test_searchMember_memberNameStartsWithS() throws Exception {
        // ## Arrange ##

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().setMemberName_LikeSearch("S", op -> op.likePrefix());
            cb.query().addOrderBy_MemberName_Asc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            assertTrue(member.getMemberName().startsWith("S"));
        }
    }

    public void test_searchMember_memberId1() throws Exception {
        // ## Arrange ##

        // ## Act ##
        Member member = memberBhv.selectEntityWithDeletedCheck(cb -> {
            cb.query().setMemberId_Equal(1);
            // 9999に変えると以下になる
            // org.dbflute.exception.EntityAlreadyDeletedException: Look! Read the message below.
            // Please confirm the existence of your target record on your database.
            // Does the target record really created before this operation?
            // Has the target record been deleted by other thread?
            // It is precondition that the record exists on your database.
            // selectEntityWithDeletedCheckは必ず1件ある状態で検索を行うで、id=9999に一致するデータがないため本当にあるか？削除されていないか？のエラー文が出ている
        });

        // ## Assert ##
        assertEquals(Integer.valueOf(1), member.getMemberId());
    }

    public void test_searchMember_birthdateIsNull() throws Exception {
        // ## Arrange ##

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().setBirthdate_IsNull();
            cb.query().addOrderBy_UpdateDatetime_Desc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            assertNull(member.getBirthdate());
        }
    }
}
