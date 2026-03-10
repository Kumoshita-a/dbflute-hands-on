package org.docksidestage.handson.exercise;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exbhv.MemberSecurityBhv;
import org.docksidestage.handson.dbflute.exbhv.PurchaseBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.dbflute.exentity.MemberSecurity;
import org.docksidestage.handson.dbflute.exentity.MemberStatus;
import org.docksidestage.handson.dbflute.exentity.Product;
import org.docksidestage.handson.dbflute.exentity.Purchase;
import org.dbflute.exception.NonSetupSelectRelationAccessException;
import org.dbflute.exception.NonSpecifiedColumnAccessException;
import org.docksidestage.handson.unit.UnitContainerTestCase;

/**
 * @author a.kumoshita
 */
public class HandsOn03Test extends UnitContainerTestCase {

    @Resource
    private MemberBhv memberBhv;

    @Resource
    private MemberSecurityBhv memberSecurityBhv;

    @Resource
    private PurchaseBhv purchaseBhv;

    /**
     * 会員名称がSで始まる1968年1月1日以前に生まれた会員を検索
     * 会員ステータスも取得する
     * 生年月日の昇順で並べる
     */
    public void test_searchMember_nameStartsWithS_bornBefore19680101() throws Exception {
        // ## Arrange ##
        // #1on1: 日付表現曖昧の話 (2026/02/24)
        // 来週の水曜まで休みますはどこまで休む話。
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
            // TODO done kumoshita ロジカルな行はできるだけスッキリ、getBirthdate()を抽出しましょう by jflute (2026/02/24)
            LocalDate birthdate = member.getBirthdate();
            assertTrue(birthdate.isBefore(targetDate) || birthdate.isEqual(targetDate));
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
            // TODO done kumoshita なかった場合、そもそもここのget()で落ちてassertNotNull()まで行かない by jflute (2026/02/24)
            // 関連テーブルはOptionalなので、Optionalのpresentを見てアサートする方が意図が正確。
            //
            // #1on1: 会員ステータスが絶対にする確証は？ (2026/02/24)
            // ER図上のカージナリティ表現、黒丸がmany-to-one側のoneに付いていないから必ず存在する by くもしたさん
            // FK制約の話。
            // NotNullかつFK制約のカラムであれば、探しに行きさえすれば必ず存在する。(黒丸が付いてない理由とも言える)
            // if文を書く理由もある一方で、if文を書かない理由も存在する。そこを明確にしておかないと。
            //
            // #1on1: 会員セキュリティが絶対にする確証は？ (2026/02/24)
            // 探しに行く方向とFKの方向が逆、物理的にはセキュリティに黒丸がないことが保証されているわけではない。
            // 業務制約(論理制約)と言える。(人間の決め事で、実際にチェックされるわけじゃない)
            // テーブルコメントに "会員とは one-to-one で、会員一人につき必ず一つのセキュリティ情報がある" と書いてある。
            //
            // 関連テーブルを取得したときは、常に「必ずあるのか？ないのか？」とその理由を確認する習慣を。
            assertTrue(member.getMemberStatus().isPresent());
            assertTrue(member.getMemberSecurityAsOne().isPresent());
            MemberStatus status = member.getMemberStatus().get();
            MemberSecurity security = member.getMemberSecurityAsOne().get();
            log("会員名称: {}, 生年月日: {}, ステータス: {}, リマインダ質問: {}",
                    member.getMemberName(), member.getBirthdate(), status.getMemberStatusName(), security.getReminderQuestion());
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
        // #1on1: ループの外で検索しているのGood // (2026/02/24)
        ListResultBean<MemberSecurity> securityList = memberSecurityBhv.selectList(cb -> {
            cb.query().setMemberId_InScope(memberIdList);
        });
        for (Member member : memberList) {
            MemberSecurity security = securityList.stream()
                    .filter(s -> s.getMemberId().equals(member.getMemberId()))
                    .findFirst().get(); // #1on1: orElseThrow()のジレンマの良い例
            String reminderQuestion = security.getReminderQuestion();
            log("会員名称: {}, リマインダ質問: {}", member.getMemberName(), reminderQuestion);
            assertTrue(reminderQuestion.contains("2"));
        }
    }

    /**
     * 会員ステータスの表示順カラムで会員を並べて検索
     * 会員ステータスのデータ自体は要らない
     * その次には、会員の会員IDの降順で並べる
     */
    public void test_searchMember_orderByMemberStatusDisplayOrder() throws Exception {
        // ## Arrange ##

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().queryMemberStatus().addOrderBy_DisplayOrder_Asc();
            cb.query().addOrderBy_MemberId_Desc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            log("会員名称: {}, ステータスコード: {}", member.getMemberName(), member.getMemberStatusCode());

            // 会員ステータスのデータが取れていないことをアサート
            assertException(NonSetupSelectRelationAccessException.class, () -> member.getMemberStatus().get());
        }
        // 会員ステータスごとに固まっていることをアサート
        // 一度離れたステータスコードが再び出現しないことを確認
        Set<String> finishedCodes = new HashSet<>();
        String previousCode = null;
        for (Member member : memberList) {
            String currentCode = member.getMemberStatusCode();
            if (!currentCode.equals(previousCode)) {
                assertFalse("会員ステータスが固まって並んでいません: " + currentCode, finishedCodes.contains(currentCode));
                if (previousCode != null) {
                    finishedCodes.add(previousCode);
                }
                previousCode = currentCode;
            }
        }
    }

    /**
     * 生年月日が存在する会員の購入を検索
     * 会員名称と会員ステータス名称と商品名を取得する
     * 購入日時の降順、購入価格の降順、商品IDの昇順、会員IDの昇順で並べる
     */
    public void test_searchPurchase_memberBirthdateExists() throws Exception {
        // ## Arrange ##

        // ## Act ##
        ListResultBean<Purchase> purchaseList = purchaseBhv.selectList(cb -> {
            cb.setupSelect_Member().withMemberStatus();
            cb.setupSelect_Product();
            cb.query().queryMember().setBirthdate_IsNotNull();
            cb.query().addOrderBy_PurchaseDatetime_Desc();
            cb.query().addOrderBy_PurchasePrice_Desc();
            cb.query().addOrderBy_ProductId_Asc();
            cb.query().addOrderBy_MemberId_Asc();
        });

        // ## Assert ##
        assertHasAnyElement(purchaseList);
        for (Purchase purchase : purchaseList) {
            Member member = purchase.getMember().get();
            MemberStatus status = member.getMemberStatus().get();
            Product product = purchase.getProduct().get();
            log("会員名称: {}, ステータス: {}, 商品名: {}", member.getMemberName(), status.getMemberStatusName(), product.getProductName());
            assertNotNull(member.getBirthdate());
        }
    }

    /**
     * 2005年10月の1日から3日までに正式会員になった会員を検索
     * 会員名称に "vi" を含む会員を検索
     * 会員ステータスも取得（ただし名称だけ）
     */
    public void test_searchMember_formalizedIn200510_01to03() throws Exception {
        // ## Arrange ##
        String fromDateStr = "2005/10/01";
        String toDateStr = "2005/10/03";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDateTime fromDate = LocalDate.parse(fromDateStr, formatter).atStartOfDay();
        LocalDateTime toDate = LocalDate.parse(toDateStr, formatter).atStartOfDay();

        // 10月1日ジャストの正式会員日時を持つ会員データを作成
        adjustMember_FormalizedDatetime_FirstOnly(fromDate, "vi");

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberStatus();
            cb.specify().specifyMemberStatus().columnMemberStatusName();
            cb.query().setFormalizedDatetime_FromTo(fromDate, toDate, op -> op.compareAsDate());
            cb.query().setMemberName_LikeSearch("vi", op -> op.likeContain());
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            MemberStatus status = member.getMemberStatus().get();
            log("会員名称: {}, 正式会員日時: {}, ステータス名称: {}", member.getMemberName(), member.getFormalizedDatetime(), status.getMemberStatusName());

            // 会員名称にviが含まれていることをアサート
            assertContains(member.getMemberName(), "vi");

            // 正式会員日時が指定範囲内であることをアサート
            LocalDateTime formalizedDatetime = member.getFormalizedDatetime();
            assertNotNull(formalizedDatetime);
            assertTrue(!formalizedDatetime.isBefore(fromDate));
            // compareAsDate なので toDate の翌日未満
            assertTrue(formalizedDatetime.isBefore(toDate.plusDays(1)));

            // 会員ステータスがコードと名称だけ取得されていることをアサート（descriptionにアクセスすると例外）
            assertException(NonSpecifiedColumnAccessException.class, () -> status.getDescription());
            assertException(NonSpecifiedColumnAccessException.class, () -> status.getDisplayOrder());
        }
    }
}
