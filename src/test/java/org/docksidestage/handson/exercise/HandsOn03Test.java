package org.docksidestage.handson.exercise;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exbhv.MemberSecurityBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.dbflute.exentity.MemberSecurity;
import org.docksidestage.handson.dbflute.exentity.MemberStatus;
import org.docksidestage.handson.unit.UnitContainerTestCase;

/**
 * @author a.kumoshita
 */
public class HandsOn03Test extends UnitContainerTestCase {

    @Resource
    private MemberBhv memberBhv;

    @Resource
    private MemberSecurityBhv memberSecurityBhv;

    /**
     * 会員名称がSで始まる1968年1月1日以前に生まれた会員を検索
     * 会員ステータスも取得する
     * 生年月日の昇順で並べる
     */
    public void test_searchMember_nameStartsWithS_bornBefore19680101() throws Exception {
        // ## Arrange ##
        LocalDate targetDate = LocalDate.of(1968, 1, 1);

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberStatus();
            cb.query().setMemberName_LikeSearch("S", op -> op.likePrefix());
            cb.query().setBirthdate_LessEqual(targetDate);
            cb.query().addOrderBy_Birthdate_Asc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            MemberStatus status = member.getMemberStatus().get();
            log("会員名称: {}, 生年月日: {}, ステータス: {}", member.getMemberName(), member.getBirthdate(), status.getMemberStatusName());
            assertTrue(member.getMemberName().startsWith("S"));
            assertTrue(member.getBirthdate().isBefore(targetDate) || member.getBirthdate().isEqual(targetDate));
        }
    }

    /**
     * 会員ステータスと会員セキュリティ情報も取得して会員を検索
     * 若い順で並べる。生年月日がない人は会員IDの昇順で並ぶようにする
     */
    public void test_searchMember_withStatusAndSecurity_orderByBirthdateDesc() throws Exception {
        // ## Arrange ##

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberStatus();
            cb.setupSelect_MemberSecurityAsOne();
            cb.query().addOrderBy_Birthdate_Desc();
            cb.query().addOrderBy_MemberId_Asc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            MemberStatus status = member.getMemberStatus().get();
            MemberSecurity security = member.getMemberSecurityAsOne().get();
            log("会員名称: {}, 生年月日: {}, ステータス: {}, リマインダ質問: {}",
                    member.getMemberName(), member.getBirthdate(), status.getMemberStatusName(), security.getReminderQuestion());
            assertNotNull(status);
            assertNotNull(security);
        }
    }

    /**
     * 会員セキュリティ情報のリマインダ質問で2という文字が含まれている会員を検索
     * 会員セキュリティ情報のデータ自体は要らない
     * リマインダ質問に2が含まれていることをアサート
     */
    public void test_searchMember_securityReminderContains2() throws Exception {
        // ## Arrange ##

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().queryMemberSecurityAsOne().setReminderQuestion_LikeSearch("2", op -> op.likeContain());
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        List<Integer> memberIdList = memberList.stream().map(Member::getMemberId).collect(java.util.stream.Collectors.toList());
        ListResultBean<MemberSecurity> securityList = memberSecurityBhv.selectList(cb -> {
            cb.query().setMemberId_InScope(memberIdList);
        });
        for (Member member : memberList) {
            MemberSecurity security = securityList.stream()
                    .filter(s -> s.getMemberId().equals(member.getMemberId()))
                    .findFirst().get();
            String reminderQuestion = security.getReminderQuestion();
            log("会員名称: {}, リマインダ質問: {}", member.getMemberName(), reminderQuestion);
            assertTrue(reminderQuestion.contains("2"));
        }
    }
}
