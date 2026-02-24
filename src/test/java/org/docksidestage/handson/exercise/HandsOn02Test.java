package org.docksidestage.handson.exercise;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.unit.UnitContainerTestCase;

// #1on1: マスタテーブルとは？どこまでがマスター？話 (2026/02/10)
// #1on1: ReplaceSchemaでの例外翻訳やデバッグ情報の導出などの話 (2026/02/10)
// DateAdjustmentなど、テストデータ登録ツールもなかなか奥が深い。
// #1on1: MySQLの中の翻訳？Field 'REGISTER_DATETIME' doesn't have a default value (2026/02/10)
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
        log("会員テストデータ件数: {}", count);
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
            log("会員名称: {}", member.getMemberName());
            assertTrue(member.getMemberName().startsWith("S"));
        }
    }

    public void test_searchMember_memberId1() throws Exception {
        // ## Arrange ##

        // ## Act ##
        // #1on1: DBFluteのOptionalのコンセプト (2026/02/10)
        // Java標準のOptionalのorElseThrow()の理想と現実のジレンマ。
        // (つどつどorElseThrow面倒問題と、orElseThrowの例外メッセージ雑問題)
        // alwaysPresent()の紹介。
        //int memberId = 9999;
        //memberBhv.selectEntity(cb -> { // as OptionalEntity
        //    cb.query().setMemberId_Equal(memberId);
        //}).alwaysPresent(mb -> {
        //    // ...
        //});
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
        log("会員ID: {}, 会員名称: {}", member.getMemberId(), member.getMemberName());
        assertEquals(Integer.valueOf(1), member.getMemberId());
    }

    // done jflute 次回1on1ふぉろーここから (2026/02/10)
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
            log("会員名称: {}, 生年月日: {}", member.getMemberName(), member.getBirthdate());
            assertNull(member.getBirthdate());
        }
    }
}
