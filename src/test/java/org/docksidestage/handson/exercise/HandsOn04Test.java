package org.docksidestage.handson.exercise;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.docksidestage.handson.dbflute.allcommon.CDef;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exbhv.PurchaseBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.dbflute.exentity.Product;
import org.docksidestage.handson.dbflute.exentity.Purchase;
import org.docksidestage.handson.unit.UnitContainerTestCase;

/**
 * @author a.kumoshita
 */
public class HandsOn04Test extends UnitContainerTestCase {

    @Resource
    private MemberBhv memberBhv;
    @Resource
    private PurchaseBhv purchaseBhv;

    // ===================================================================================
    //                                                                          ベタベタのやり方
    //                                                                          ============
    /**
     * 退会会員の未払い購入を検索
     * <pre>
     * o 退会会員のステータスコードは "WDL"。ひとまずベタで
     * o 支払完了フラグは "0" で未払い。ひとまずベタで
     * o 購入日時の降順で並べる
     * o 会員名称と商品名と一緒にログに出力
     * o 購入が未払いであることをアサート
     * </pre>
     */
    public void test_searchPurchase_unpaid_byWithdrawalMember() throws Exception {
        // ## Arrange ##

        // ## Act ##
        ListResultBean<Purchase> purchaseList = purchaseBhv.selectList(cb -> {
            cb.setupSelect_Member();
            cb.setupSelect_Product();
            cb.query().queryMember().setMemberStatusCode_Equal_退会会員();
            cb.query().setPaymentCompleteFlg_Equal_False();
            cb.query().addOrderBy_PurchaseDatetime_Desc();
        });

        // ## Assert ##
        assertHasAnyElement(purchaseList);
        for (Purchase purchase : purchaseList) {
            Member member = purchase.getMember().get();
            Product product = purchase.getProduct().get();
            log("会員名称: {}, 商品名: {}, 購入日時: {}", member.getMemberName(), product.getProductName(), purchase.getPurchaseDatetime());
            assertEquals(CDef.Flg.False, purchase.getPaymentCompleteFlgAsFlg());
        }
    }

    /**
     * 会員退会情報も取得して会員を検索
     * <pre>
     * o 退会会員でない会員は、会員退会情報を持っていないことをアサート
     * o 退会会員のステータスコードは "WDL"。ひとまずベタで
     * o 不意のバグや不意のデータ不備でもテストが(できるだけ)成り立つこと
     * </pre>
     */
    public void test_searchMember_withMemberWithdrawal() throws Exception {
        // ## Arrange ##

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberWithdrawalAsOne();
            cb.query().addOrderBy_MemberId_Asc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        boolean foundWithdrawal = false;
        boolean foundNonWithdrawal = false;
        for (Member member : memberList) {
            boolean isWithdrawal = member.isMemberStatusCode退会会員();
            boolean hasWithdrawal = member.getMemberWithdrawalAsOne().isPresent();
            log("会員名称: {}, ステータスコード: {}, 退会情報: {}", member.getMemberName(), member.getMemberStatusCode(),
                    hasWithdrawal ? "あり" : "なし");
            if (!isWithdrawal) {
                assertFalse(hasWithdrawal);
            } else {
                foundWithdrawal = true;
            }
            if (!isWithdrawal) {
                foundNonWithdrawal = true;
            }
        }
        assertTrue(foundWithdrawal);
        assertTrue(foundNonWithdrawal);
    }
}
