package org.docksidestage.handson.exercise;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.docksidestage.handson.dbflute.allcommon.CDef;
import org.docksidestage.handson.dbflute.exbhv.MemberAddressBhv;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exbhv.PurchaseBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.dbflute.exentity.MemberAddress;
import org.docksidestage.handson.dbflute.exentity.MemberLogin;
import org.docksidestage.handson.dbflute.exentity.Purchase;
import org.docksidestage.handson.dbflute.exentity.Region;
import org.docksidestage.handson.unit.UnitContainerTestCase;

/**
 * @author a.kumoshita
 */
public class HandsOn05Test extends UnitContainerTestCase {

    @Resource
    private MemberBhv memberBhv;
    @Resource
    private PurchaseBhv purchaseBhv;
    @Resource
    private MemberAddressBhv memberAddressBhv;

    // ===================================================================================
    //                                                                       業務的one-to-oneとは？
    //                                                                       ====================
    /**
     * 会員住所を検索 (業務的one-to-one定義の前: referrer/many-to-oneで素朴に)
     * <pre>
     * o 会員名称・有効期間(開始〜終了)・住所・地域名をログに出力
     * o 会員IDの昇順、有効開始日の降順で並べる
     * o 会員と地域がそれぞれ取得できていることをアサート
     * </pre>
     */
    public void test_searchMemberAddress_list() throws Exception {
        // ## Arrange ##

        // ## Act ##
        ListResultBean<MemberAddress> addressList = memberAddressBhv.selectList(cb -> {
            cb.setupSelect_Member();
            cb.setupSelect_Region();
            cb.query().addOrderBy_MemberId_Asc();
            cb.query().addOrderBy_ValidBeginDate_Desc();
        });

        // ## Assert ##
        assertHasAnyElement(addressList);
        for (MemberAddress address : addressList) {
            Member member = address.getMember().get();
            Region region = address.getRegion().get();
            log("会員名称: {}, 有効期間: {} 〜 {}, 住所: {}, 地域: {}", member.getMemberName(),
                    address.getValidBeginDate(), address.getValidEndDate(), address.getAddress(), region.getRegionName());
            assertTrue(address.getMember().isPresent());
            assertTrue(address.getRegion().isPresent());
        }
    }

    // ===================================================================================
    //                                                                    業務的one-to-oneを利用した実装
    //                                                                    ======================
    /**
     * 現在住所付きで会員を検索 (業務的one-to-one: MemberAddressAsValid)
     * <pre>
     * o 業務的one-to-oneの仮想FK FK_MEMBER_MEMBER_ADDRESS_VALID を利用
     * o 現在日時時点で有効な住所 = 現在住所。基底クラスの現在日時メソッド currentLocalDate() を使う
     * o 現在住所を持たない会員は紛れになるので、existsで「現在有効な住所を持つ会員」だけに絞る
     * o 会員名称・現在住所・有効期間をログに出力
     * o 絞り込んだ各会員が現在住所(AsValid)を保持していることをアサート
     * </pre>
     */
    public void test_searchMember_withCurrentAddress() throws Exception {
        // ## Arrange ##
        // fixedConditionの /*targetDate(Date)*/ は DATE 型なので生成シグネチャは LocalDate
        LocalDate targetDate = currentLocalDate();

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberAddressAsValid(targetDate).withRegion();
            // 現在有効な住所を持つ会員だけに絞る (AsValidのfixedConditionと同じ条件)
            cb.query().existsMemberAddress(addressCB -> {
                addressCB.query().setValidBeginDate_LessEqual(targetDate);
                addressCB.query().setValidEndDate_GreaterEqual(targetDate);
            });
            cb.query().addOrderBy_MemberId_Asc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            // 紛れのgreen回避: 絞り込んだ会員は必ず現在住所を持つ
            assertTrue(member.getMemberAddressAsValid().isPresent());
            MemberAddress address = member.getMemberAddressAsValid().get();
            Region region = address.getRegion().get();
            log("会員名称: {}, 現在住所: {} ({}), 有効期間: {} 〜 {}", member.getMemberName(), address.getAddress(),
                    region.getRegionName(), address.getValidBeginDate(), address.getValidEndDate());
        }
    }

    /**
     * 千葉県に現在住んでいる会員の購入を検索 (業務的one-to-one + 千葉で絞り込み)
     * <pre>
     * o 現在住所(MemberAddressAsValid)の地域が「千葉」の会員の購入だけを検索
     * o 会員名称・会員ステータス・現在住所をログに出力
     * o 各購入の会員の現在住所が千葉であることをアサート
     * </pre>
     */
    public void test_searchPurchase_byChibaResident() throws Exception {
        // ## Arrange ##
        LocalDate targetDate = currentLocalDate();

        // ## Act ##
        ListResultBean<Purchase> purchaseList = purchaseBhv.selectList(cb -> {
            cb.setupSelect_Member().withMemberStatus();
            cb.setupSelect_Member().withMemberAddressAsValid(targetDate).withRegion();
            // #1on1: CDefじゃなくてで setRegionId_Equal_千葉() でもOK (2026/06/23)
            cb.query().queryMember().queryMemberAddressAsValid(targetDate).setRegionId_Equal_AsRegion(CDef.Region.千葉);
            cb.query().addOrderBy_PurchaseDatetime_Desc();
        });

        // ## Assert ##
        assertHasAnyElement(purchaseList);
        for (Purchase purchase : purchaseList) {
            Member member = purchase.getMember().get();
            MemberAddress address = member.getMemberAddressAsValid().get();
            log("会員名称: {}, ステータス: {}, 現在住所: {} ({})", member.getMemberName(),
                    member.getMemberStatus().get().getMemberStatusName(), address.getAddress(),
                    address.getRegion().get().getRegionName());
            assertTrue(address.isRegionId千葉());
            assertEquals(CDef.Region.千葉, address.getRegionIdAsRegion());
        }
    }

    // ===================================================================================
    //                                                                    導出的one-to-oneを利用した実装
    //                                                                    ======================
    /**
     * 最新ログイン付きで会員を検索 (導出的one-to-one: MemberLoginAsLatest)
     * <pre>
     * o 導出的one-to-oneの仮想FK FK_MEMBER_MEMBER_LOGIN_LATEST を利用 (相関サブクエリでmax(LOGIN_DATETIME))
     * o ログイン時点のステータス(LOGIN_MEMBER_STATUS_CODE)も一緒に取得
     * o ログインを持たない会員は紛れになるので existsMemberLogin で絞る
     * o 会員名称・最新ログイン日時・ログイン時ステータス名をログに出力
     * o 期待値は Act と独立に: LoadReferrer で全ログインをロードし、AsLatest が本当に最大日時の1件であることをアサート
     * </pre>
     */
    public void test_searchMember_withLatestLogin() throws Exception {
        // ## Arrange ##

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberLoginAsLatest().withMemberStatus();
            cb.query().existsMemberLogin(loginCB -> {}); // ログイン実績のある会員だけ
            cb.query().addOrderBy_MemberId_Asc();
        });
        // 期待値導出のため、各会員の全ログインを1回の追加クエリでロード (n+1回避)
        memberBhv.loadMemberLogin(memberList, loginCB -> {});

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            // 紛れのgreen回避: ログインありに絞ったので必ず最新ログインを持つ
            assertTrue(member.getMemberLoginAsLatest().isPresent());
            MemberLogin latest = member.getMemberLoginAsLatest().get();
            assertNotNull(latest.getLoginDatetime());
            log("会員名称: {}, 最新ログイン日時: {}, ログイン時ステータス: {}", member.getMemberName(),
                    latest.getLoginDatetime(), latest.getMemberStatus().get().getMemberStatusName());

            // 導出的one-to-oneが本当に「最新」を選んでいるか、独立に求めた最大日時と照合
            LocalDateTime expectedMaxDatetime = member.getMemberLoginList().stream()
                    .map(MemberLogin::getLoginDatetime)
                    .max(Comparator.naturalOrder())
                    .get(); // Java8なのでorElseThrow(引数なし)はまだ使えない
            assertEquals(expectedMaxDatetime, latest.getLoginDatetime());
        }
    }
    
    // #1on1: 現場での業務的one-to-oneを見てみた (2026/06/23)
    // OverRelation の話も紹介。
    // 業務的many-to-oneの話も紹介。
    
    // #1on1: テストデータの登録時チェックはまだ (2026/06/23)
}
