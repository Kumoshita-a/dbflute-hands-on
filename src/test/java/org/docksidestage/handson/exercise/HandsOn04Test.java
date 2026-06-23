package org.docksidestage.handson.exercise;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.dbflute.optional.OptionalThing;
import org.docksidestage.handson.dbflute.allcommon.CDef;
import org.docksidestage.handson.dbflute.cbean.cq.bs.AbstractBsMemberCQ;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exbhv.PurchaseBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.dbflute.exentity.MemberStatus;
import org.docksidestage.handson.dbflute.exentity.MemberWithdrawal;
import org.docksidestage.handson.dbflute.exentity.Product;
import org.docksidestage.handson.dbflute.exentity.ProductStatus;
import org.docksidestage.handson.dbflute.exentity.Purchase;
import org.docksidestage.handson.dbflute.exentity.WithdrawalReason;
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
        // #1on1: 反対の事象もアサートしないと成り立たないっての最初から実装できてるの素晴らしい (2026/04/28)
        // ここぞってところ紛れのgreenに絶対にならないように。
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

    // ===================================================================================
    //                                                                       区分値メソッドを使って実装
    //                                                                       ====================
    /**
     * 一番若い仮会員の会員を検索
     */
    public void test_searchMember_youngestProvisional() throws Exception {
        // ## Arrange ##

        // ## Act ##
        Member member = memberBhv.selectEntity(cb -> {
            cb.setupSelect_MemberStatus();
            cb.query().setMemberStatusCode_Equal_仮会員();
            // #1on1: もし同率首位を取るとなったら、ここでもScalarCondition。 (2026/04/28)
            //cb.query().scalar_Equal().max(memberCB -> {
            //    memberCB.specify().columnBirthdate();
            //    memberCB.query().setMemberStatusCode_Equal_仮会員();
            //});
            cb.query().setBirthdate_IsNotNull();
            cb.query().addOrderBy_Birthdate_Desc();
            // #1on1: かぶる可能性があるので1件を保証してるのGood (2026/04/28)
            // 何が取れるのか？の保証は厳密にない。DBMSの実装に寄る。(ランダムって思ってた方が良い)
            // (実際には、MySQLとかだとinsert順が影響すること多いけど、そこに頼っちゃダメ)
            cb.fetchFirst(1);
        }).get();

        // ## Assert ##
        MemberStatus status = member.getMemberStatus().get();
        log("会員名称: {}, 生年月日: {}, ステータス名称: {}", member.getMemberName(), member.getBirthdate(), status.getMemberStatusName());
        assertTrue(member.isMemberStatusCode仮会員());
    }

    /**
     * 支払済みの購入の中で一番若い正式会員のものだけ検索
     */
    public void test_searchPurchase_paid_byYoungestFormalizedMember() throws Exception {
        // ## Arrange ##

        // ## Act ##
        // #1on1: 外側と内側で同じ条件入れないと辻褄合わない部分うまくできててGood (2026/04/28)
        ListResultBean<Purchase> purchaseList = purchaseBhv.selectList(cb -> {
            cb.setupSelect_Member().withMemberStatus();
            cb.query().setPaymentCompleteFlg_Equal_True();
            cb.query().queryMember().setMemberStatusCode_Equal_正式会員();
            cb.query().queryMember().scalar_Equal().max(memberCB -> {
                memberCB.specify().columnBirthdate();
                memberCB.query().setMemberStatusCode_Equal_正式会員();
                // done kumoshita 慣習として、pCB ではなく、purchaseCB (テーブルのキーワード) by jflute (2026/04/28)
                // done kumoshita 慣習として、関連テーブルのLambdaは、blockスタイルのLambdaで by jflute (2026/04/28)
                memberCB.query().existsPurchase(purchaseCB -> {
                    purchaseCB.query().setPaymentCompleteFlg_Equal_True();
                });
                // #1on1: こういうの考える時は、コードで考えるのではなく、図とか表とか (2026/04/28)
                // (手元の紙で考えたということでGood)
                // // ホワイトボードを買ってこよう
                // https://jflute.hatenadiary.jp/entry/20110607/1307440686
                // 人って、書いてるプロセスをみてる方が理解しやすい by くもしたさん
                // 思考のプロセスがそのまま見えるのかも by くぼ
                // ライブコーディングにもつながるかも by くぼ
                // 自分で書いたことないのにAIのコードひたすら読む辛い話にもつながるかも by くぼ
            });
            cb.query().addOrderBy_PurchaseDatetime_Desc();
        });

        // ## Assert ##
        assertHasAnyElement(purchaseList);
        Integer firstMemberId = purchaseList.get(0).getMemberId();
        for (Purchase purchase : purchaseList) {
            Member member = purchase.getMember().get();
            MemberStatus status = member.getMemberStatus().get();
            log("会員名称: {}, 生年月日: {}, ステータス名称: {}, 購入日時: {}", member.getMemberName(), member.getBirthdate(),
                    status.getMemberStatusName(), purchase.getPurchaseDatetime());
            assertTrue(member.isMemberStatusCode正式会員());
            // 「一番若い正式会員のものだけ」 → 全件同じ会員
            assertEquals(firstMemberId, purchase.getMemberId());
        }
    }

    /**
     * 生産販売可能な商品の購入を検索
     */
    public void test_searchPurchase_byOnSaleProductionProduct() throws Exception {
        // ## Arrange ##

        // ## Act ##
        ListResultBean<Purchase> purchaseList = purchaseBhv.selectList(cb -> {
            cb.setupSelect_Product().withProductStatus();
            cb.setupSelect_Member().withMemberWithdrawalAsOne().withWithdrawalReason();
            cb.query().queryProduct().setProductStatusCode_Equal_生産販売可能();
            cb.query().addOrderBy_PurchasePrice_Desc();
        });

        // ## Assert ##
        assertHasAnyElement(purchaseList);
        for (Purchase purchase : purchaseList) {
            Product product = purchase.getProduct().get();
            ProductStatus productStatus = product.getProductStatus().get();
            String withdrawalReasonText = purchase.getMember()
                    .flatMap(Member::getMemberWithdrawalAsOne)
                    .flatMap(MemberWithdrawal::getWithdrawalReason)
                    .map(WithdrawalReason::getWithdrawalReasonText)
                    .orElse("none");
            log("商品名: {}, 商品ステータス名称: {}, 購入価格: {}, 退会理由: {}", product.getProductName(),
                    productStatus.getProductStatusName(), purchase.getPurchasePrice(), withdrawalReasonText);
            assertTrue(product.isProductStatusCode生産販売可能());
        }
    }

    /**
     * 正式会員と退会会員の会員を検索
     */
    public void test_searchMember_formalAndWithdrawal_changeEntityOnly() throws Exception {
        // ## Arrange ##

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberStatus();
            // #1on1: 一応紹介でorScopeQuery()を使った例、でもInScopeの方がベター (目的が絞られてる) (2026/04/28)
            // 目的が絞られてると、実行計画とかで良い方を選んでくれる確率が高くなる。
            // (このケースそんな変わらないけど、そういう発想を持っていて欲しい)
            // orScopeQueryの方が汎用的、InScopeの方が狭まっている。
            // フィットするなら狭まっている方を使う方が意図が伝わりやすい。
            //cb.orScopeQuery(orCB -> {
            //    orCB.query().setMemberStatusCode_Equal_正式会員();
            //    orCB.query().setMemberStatusCode_Equal_退会会員();
            //});
            cb.query().setMemberStatusCode_InScope_AsMemberStatus(
                    Arrays.asList(CDef.MemberStatus.正式会員, CDef.MemberStatus.退会会員));
            cb.query().queryMemberStatus().addOrderBy_DisplayOrder_Asc();
            cb.query().addOrderBy_MemberId_Asc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        boolean foundFormalized = false;
        boolean foundWithdrawal = false;
        for (Member member : memberList) {
            log("会員名称: {}, ステータス: {}", member.getMemberName(), member.getMemberStatus().get().getMemberStatusName());
            assertTrue(member.isMemberStatusCode正式会員() || member.isMemberStatusCode退会会員());
            if (member.isMemberStatusCode正式会員()) {
                foundFormalized = true;
            }
            if (member.isMemberStatusCode退会会員()) {
                foundWithdrawal = true;
            }
        }
        assertTrue(foundFormalized);
        assertTrue(foundWithdrawal);

        // Entity 上だけで正式会員→退会会員に変更
        Member target = memberList.stream()
                .filter(Member::isMemberStatusCode正式会員)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("正式会員が含まれていない"));
        Integer targetId = target.getMemberId();
        target.setMemberStatusCodeAsMemberStatus(CDef.MemberStatus.退会会員);
        assertTrue(target.isMemberStatusCode退会会員());

        // DB 上は変更されていないこと
        Member fromDb = memberBhv.selectByPK(targetId).get();
        assertTrue(fromDb.isMemberStatusCode正式会員());
        
        // #1on1: DBFluteは、あくまで bhv で update とかやらない限り、DBは更新しない。 (2026/04/28)
        // どんだけ Entity をいじっても、それはJavaのメモリ上の変数を変えているだけ。
        // 世の中には、Entityの値を変更したら、DBも変更されるツールもある。
        //
        // DBFluteは、どこまで機能が豊富だとしても、RDBを意識したラッパーであることは変わらない。
        // 他のO/Rマッパーは、RDBを隠蔽して、あたかもODBにアクセスしているかのような実装をコンセプトにするものもある。
        
        // #1on1: LazyLoadのお話も (2026/04/28)
        // DBFluteは、LazyLoadはしない。(意図的にやらないようにしている。要望があっても断ってる)
        // LazyLoadは、n+1問題を発生させやすい機能である。
    }

    // done jflute 次回1on1, ArrangeQueryのお話 (2026/04/28)
    // #1on1: 相対的に使える部品になる。ConditionBeanじゃないと厳しい。 (2026/05/26)
    // //String sql = "select * from MEMBER mb where mb.xxxxxx like '...'";
    // new PurchaseCB().query().queryMember().arrangePaidByBankTransfer();
    /**
     * 銀行振込で購入を支払ったことのある、会員ステータスごとに一番若い会員を検索
     */
    public void test_searchMember_youngestPerStatus_paidByBankTransfer() throws Exception {
        // ## Arrange ##

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberStatus();
            cb.query().existsPurchase(purchaseCB -> {
                purchaseCB.query().existsPurchasePayment(purchasePaymentCB -> purchasePaymentCB.query().setPaymentMethodCode_Equal_BankTransfer());
            });
            // #1on1: これはなくても結果変わらない (2026/05/12)
            //cb.query().setBirthdate_IsNotNull();
            cb.query().scalar_Equal().max(memberCB -> {
                memberCB.specify().columnBirthdate();
                memberCB.query().existsPurchase(purchaseCB -> {
                    purchaseCB.query().existsPurchasePayment(purchasePaymentCB -> {
                        purchasePaymentCB.query().setPaymentMethodCode_Equal_BankTransfer();
                    });
                });
            }).partitionBy(scalarCB -> {
                scalarCB.specify().columnMemberStatusCode();
            });
            cb.query().queryMemberStatus().addOrderBy_DisplayOrder_Asc();
        });

        // ## Assert ##
        for (Member member : memberList) {
            log("会員名称: {}, ステータス: {}, 生年月日: {}", member.getMemberName(),
                    member.getMemberStatus().get().getMemberStatusName(), member.getBirthdate());
        }
        // 想定: 銀行振込実績ありステータス分の会員が取れる (3 ステータス想定だが実データ次第なので 1 以上（空でないこと）をアサート)
        assertTrue(!memberList.isEmpty());
        // ステータスごとに 1 名であることも確認
        // done kumoshita これはこれでmemberListの構造をチェックしているのはGood... by jflute (2026/05/12)
        // ただ、期待値をActの成果物(memberList)から求めても少し動作確認の精度として弱いので、
        // Actと無関係に期待値を求めてアサートしたいところ。
        // ArrangeQuery を利用して「銀行振込で支払った全会員」を別クエリで取得し、
        // Java の Stream でステータスごとの最年少 (Birthdate最大) を抽出する。
        // SQL の MAX() は NULL を無視するので、Java 側でも Birthdate NULL は集計対象から除外。
        ListResultBean<Member> paidByBankTransferAll = memberBhv.selectList(cb -> {
            cb.query().arrangePaidByBankTransfer();
        });
        assertTrue(!paidByBankTransferAll.isEmpty());

        // #1on1: まさしくロジックで求めた。Good。 (2026/05/26)
        Map<String, Optional<Member>> expectedYoungestPerStatus = paidByBankTransferAll.stream()
                .filter(m -> m.getBirthdate() != null)
                .collect(Collectors.groupingBy(
                        Member::getMemberStatusCode,
                        Collectors.maxBy(Comparator.comparing(Member::getBirthdate))));

        Set<Integer> expectedMemberIds = expectedYoungestPerStatus.values().stream()
                .map(opt -> opt.get()) // Java8なのでorElseThrow(引数なし)はまだ使えない
                .map(Member::getMemberId)
                .collect(Collectors.toSet());

        Set<Integer> actualMemberIds = memberList.stream()
                .map(Member::getMemberId)
                .collect(Collectors.toSet());

        // #1on1: もし、期待値を固定値にするパターンだったら 1,24,84 ...キツイ (2026/05/26)
        assertEquals(expectedMemberIds, actualMemberIds);
        assertEquals(expectedMemberIds.size(), memberList.size()); // Set化で重複が潰れていないことの担保
        // #1on1: もし件数だけのアサートを想定した場合、みんなのよくあるパターン (2026/05/26)
        //  assertEquals(3, memberList.size());
        //  assertEquals(CDef.MemberStatus.listAll().size(), memberList.size());
        // ↑MEMBER_STATUSしか見てないので、MEMBERテーブルのテストデータの変更で影響されやすい。
        //  assertEquals([MEMBERが存在するMemberStatusの件数], memberList.size());
        // ↑求め方:
        //   A. MemberStatusを基点にして、existsMember()
        //   B. ScalarSelectを使って、直接MEMBERから種類数を取る
        //  Integer countDistinct = memberBhv.selectScalar(Integer.class).countDistinct(cb -> {
        //      cb.specify().columnMemberStatusCode();
        //  });
        // まあ、もっと厳密には、これに、arrangeの条件まで入れていく。
        // ほぼおうむ返し条件になるけど。

        // #1on1: UnitTestの期待値をどう求めるか？連動性 (変化への対応) をどこまでやりきるか？ (2026/05/12)
        // 現場によっては、3と入れましょうな現場もある。
        // 逆に、もう少し連動性を持って、ある程度の変化が起きてもアサートが壊れないようにしましょうな現場もある。
        //
        // 今の現場では？ → 固定値を指定する → 人間がもう頭の中で期待値を導出している
        //
        // 固定値を指定:
        // o よほど複雑じゃなければ頭の中で期待値がすぐ求めるので書くのは楽
        // o 変化に弱い (間接的な変化でも修正の必要が出てきたり) // テストは変化に強い必要性がない？
        // o 可読性: なんの値？になる？...一方で、時には直感的になることも
        //   → 3じゃなくて、statusDistinctCountみたいな名前で工夫？
        //   → "正式会員" は意味がわかる。3は意味がわからん。
        //
        // プログラムで導出:
        // o 書くのは大変
        // o 変化に強い
        // o 可読性: プログラムでストーリーがわかる、一方で、プログラムを読まないといけない (視認性は悪い!?)
        // o 期待値の導出でバグが入る可能性がある (キリがない問題、テストのテストは？)
        //
        // ↑の二つ、グラデーションになる。
        //
        // テストは変化に強い必要性がない？
        // → jfluteの経験上、あまりに変化に弱くて影響が大きいと、落ちたテストを放置し始める
        // → E2Eも似た話、もっと大変かも
    }

    /**
     * 銀行振込で購入を支払ったことのある、会員ステータスごとに一番若い会員を検索 (ArrangeQuery 版)
     * <p>
     * MemberCQ#arrangePaidByBankTransfer() で「銀行振込で購入を支払った」条件を再利用。
     * </p>
     */
    public void test_searchMember_youngestPerStatus_paidByBankTransfer_arrangeQuery() throws Exception {
        // ## Arrange ##

        // ## Act ##
        // #1on1 ArrangeQueryのコンセプト (2026/05/12)
        // 現場での活用のされ方。
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberStatus();
            cb.query().arrangePaidByBankTransfer();
            cb.query().setBirthdate_IsNotNull();
            cb.query().scalar_Equal().max(memberCB -> {
                memberCB.specify().columnBirthdate();
                memberCB.query().arrangePaidByBankTransfer();
            }).partitionBy(scalarCB -> {
                scalarCB.specify().columnMemberStatusCode();
            });
            cb.query().queryMemberStatus().addOrderBy_DisplayOrder_Asc();
        });

        // ## Assert ##
        for (Member member : memberList) {
            log("会員名称: {}, ステータス: {}, 生年月日: {}", member.getMemberName(),
                    member.getMemberStatus().get().getMemberStatusName(), member.getBirthdate());
        }
        assertTrue(!memberList.isEmpty());

        // 期待値は Act と独立に: ArrangeQuery で「銀行振込で支払った全会員」を取って
        // MemberId の Set を比較する。MAX(NULL) 無視と合わせるため Birthdate NULL は除外。
        ListResultBean<Member> paidByBankTransferAll = memberBhv.selectList(cb -> {
            cb.query().arrangePaidByBankTransfer();
        });
        assertTrue(!paidByBankTransferAll.isEmpty());

        Map<String, Optional<Member>> expectedYoungestPerStatus = paidByBankTransferAll.stream()
                .filter(m -> m.getBirthdate() != null)
                .collect(Collectors.groupingBy(
                        Member::getMemberStatusCode,
                        Collectors.maxBy(Comparator.comparing(Member::getBirthdate))));

        Set<Integer> expectedMemberIds = expectedYoungestPerStatus.values().stream()
                .map(opt -> opt.get())
                .map(Member::getMemberId)
                .collect(Collectors.toSet());

        Set<Integer> actualMemberIds = memberList.stream()
                .map(Member::getMemberId)
                .collect(Collectors.toSet());

        assertEquals(expectedMemberIds, actualMemberIds);
        assertEquals(expectedMemberIds.size(), memberList.size());
    }

    /*
     * 追加した "ハンズオン" 区分値を ConditionBean で使えることを確認
     * <pre>
     * o MEMBER_STATUS に HAN レコードを足したことで CDef.MemberStatus.ハンズオン が自動生成されている
     * o setMemberStatusCode_Equal_ハンズオン() も同時に生成されているのでそれを使う
     * o ハンズオンの会員データは投入していないので検索結果は0件である
     * </pre>
     */
    /*
    public void test_searchMember_byAddedHandsOnStatus() throws Exception {
        // ## Arrange ##

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberStatus();
            cb.query().setMemberStatusCode_Equal_ハンズオン();
            cb.query().addOrderBy_MemberId_Asc();
        });

        // ## Assert ##
        assertHasZeroElement(memberList);
        log("CDef.MemberStatus.ハンズオン alias: {}, code: {}", CDef.MemberStatus.ハンズオン.alias(),
                CDef.MemberStatus.ハンズオン.code());
    }
    */
    // セクション 4「区分値の追加と変更」エクササイズの記録
    // MEMBER_STATUS テーブルの TSV に "HAN/ハンズオン" レコードを一時追加して、ReplaceSchema → JDBC → Doc → Generate を回す。
    // 結果、CDef.MemberStatus.ハンズオン と setMemberStatusCode_Equal_ハンズオン() が自動生成され、上記テストメソッドで実際にタイプセーフな検索が組めることを確認した。
    // その後 TSV から HAN レコードを戻して再生成すると、当該区分値メソッドが消えて、
    // このテストメソッドの 3 行 (setMemberStatusCode_Equal_ハンズオン() と CDef.MemberStatus.ハンズオン × 2)が漏れなくコンパイルエラーになった。
    
    // #1on1: 現場での区分値要素の削除の仕方 (2026/05/26)
    // テーブルの構造的に referrer がいないかどうか？
    // 業務的にその要素を指定しているところがないか？ (試しに削除して自動生成してみるといい)
    
    // #1on1: CraftDiffの紹介。現場では使ってる (2026/05/26)


    /**
     * ネイティヴ型の区分値設定メソッドが不可視化(protected)されていることを確認
     * <pre>
     * o setMemberStatusCode_Equal(String) は protected (ベタなString指定を抑止)
     * o setMemberStatusCode_Equal_AsMemberStatus(CDef.MemberStatus) は public (タイプセーフ)
     * </pre>
     */
    public void test_nativeTypeSetter_isHidden() throws Exception {
        // ## Arrange ##
        Class<?> cqType = AbstractBsMemberCQ.class;

        // ## Act ##
        // #1on1: getMethod()とgetDeclaredMethod()の違い (2026/06/09)
        // DBFluteだと、DfReflectionUtil.getWholeMethod();
        // getAccessibleMethod() や、flexible のメソッドのコードもちょっと読んでみた。
        Method nativeSetter = cqType.getDeclaredMethod("setMemberStatusCode_Equal", String.class);
        Method typeSafeSetter = cqType.getDeclaredMethod("setMemberStatusCode_Equal_AsMemberStatus", CDef.MemberStatus.class);

        // ## Assert ##
        // #1on1: よくぞやってくれたわが強者よ (2026/06/09)
        log("native setter: {}, typesafe setter: {}", Modifier.toString(nativeSetter.getModifiers()),
                Modifier.toString(typeSafeSetter.getModifiers()));
        // ネイティヴ型(String)指定は外から呼べない(protected)
        assertTrue(Modifier.isProtected(nativeSetter.getModifiers()));
        assertFalse(Modifier.isPublic(nativeSetter.getModifiers()));
        // タイプセーフ版は公開されている(public)
        assertTrue(Modifier.isPublic(typeSafeSetter.getModifiers()));
    }


    /**
     * 未定義の区分値コードはタイプセーフに解決されない(空)ことを確認
     * <pre>
     * o classificationUndefinedHandlingType=EXCEPTION 設定下での未定義コードの扱い
     * o 実行時 EXCEPTION 化の本体検証は上記 ReplaceSchema デモ(記録コメント)で担保
     * </pre>
     */
    public void test_undefinedClassification_returnsEmpty() throws Exception {
        // ## Arrange ##

        // ## Act ##
        OptionalThing<CDef.MemberStatus> undefined = CDef.MemberStatus.of("XXX");
        OptionalThing<CDef.MemberStatus> defined = CDef.MemberStatus.of("FML");

        // ## Assert ##
        log("undefined 'XXX' present: {}, defined 'FML': {}", undefined.isPresent(),
                defined.map(CDef.MemberStatus::alias).orElse("none"));
        assertFalse(undefined.isPresent());
        assertTrue(defined.isPresent());
        assertEquals(CDef.MemberStatus.正式会員, defined.get());
    }


    /**
     * サービスを利用できる会員(グルーピング)を検索
     * <pre>
     * o グルーピング判定の検索メソッド setMemberStatusCode_InScope_ServiceAvailable() を使う
     * o 会員ステータスの表示順で並べる
     * o 取得会員が全員「サービス利用可能」グループ(正式会員 or 仮会員)であることをアサート
     * o 退会会員が含まれないこと、正式会員・仮会員の両方が取れていることをアサート(紛れのgreen回避)
     * </pre>
     */
    public void test_searchMember_byServiceAvailableGroup() throws Exception {
        // ## Arrange ##

        // ## Act ##
        // #1on1: その列挙のif文、見かけたら、ちょっと待った！話 (2026/06/09)
        // 再利用の第一歩の思考。業務的な抽象化をして、小さな業務概念を何かしらの方法で表現したい。
        // DBFluteだと、それがdfpropで定義してメソッドを自動生成。
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberStatus();
            cb.query().setMemberStatusCode_InScope_ServiceAvailable();
            cb.query().queryMemberStatus().addOrderBy_DisplayOrder_Asc();
            cb.query().addOrderBy_MemberId_Asc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        boolean foundFormalized = false;
        boolean foundProvisional = false;
        for (Member member : memberList) {
            MemberStatus status = member.getMemberStatus().get();
            log("会員名称: {}, ステータス: {}, グループ所属: {}", member.getMemberName(), status.getMemberStatusName(),
                    member.isMemberStatusCode_ServiceAvailable());
            // グルーピング判定(エンティティ)で「サービス利用可能」グループに属している
            assertTrue(member.isMemberStatusCode_ServiceAvailable());
            // 正式会員 or 仮会員 のみ。退会会員は含まれない
            assertTrue(member.isMemberStatusCode正式会員() || member.isMemberStatusCode仮会員());
            assertFalse(member.isMemberStatusCode退会会員());
            if (member.isMemberStatusCode正式会員()) {
                foundFormalized = true;
            }
            if (member.isMemberStatusCode仮会員()) {
                foundProvisional = true;
            }
        }
        assertTrue(foundFormalized);
        assertTrue(foundProvisional);
    }


    /**
     * 姉妹コードで未払い(false)を判定する private ヘルパー。
     * @return 未払いを表す Flg (CDef.Flg.False)。姉妹コード "false" から解決する。
     */
    private CDef.Flg provideUnpaidFlg() {
        return CDef.Flg.of("false").get(); // Java8なのでorElseThrow(引数なし)はまだ使えない
    }

    /**
     * 未払い購入のある会員を検索 (姉妹コード + LoadReferrer)
     * <pre>
     * o 支払フラグの未払い(false)を姉妹コード経由で解決した Flg で絞り込む
     * o 正式会員日時の降順(null後方) → 会員IDの昇順で並べる
     * o LoadReferrer で未払い購入を1回の追加クエリでロード(n+1を避ける)
     * o 各会員が未払い購入を保持していること、購入が未払い(False)であることをアサート
     * </pre>
     */
    public void test_searchMember_havingUnpaidPurchase_bySisterCode() throws Exception {
        // ## Arrange ##
        // #1on1: 姉妹コードの使い所の話(ささいではあるけれど) (2026/06/09)
        CDef.Flg unpaidFlg = provideUnpaidFlg();
        // 姉妹コード "false" が Flg.False に解決されること
        assertEquals(CDef.Flg.False, unpaidFlg);

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().existsPurchase(purchaseCB -> {
                purchaseCB.query().setPaymentCompleteFlg_Equal_AsFlg(unpaidFlg);
            });
            // 正式会員日時の降順、null は後方へ
            cb.query().addOrderBy_FormalizedDatetime_Desc().withNullsLast();
            cb.query().addOrderBy_MemberId_Asc();
        });
        // #1on1: O/Rマッパー業界では鬼門のone-to-manyの扱い (2026/06/09)
        // n+1問題、1人1人の会員ごとにその人のpurchasesを取ると、n+1のSQL発行回数になっちゃう。
        // パフォーマンスチューニングのお仕事の昔話。1ボタンで500回SQLが流れてた。
        // LoadReferrerは、1+1+1...にした。many側テーブル1つにつき1回のSQL。
        // (超最速ではないけど、すごく遅くなるってことはなく安定する)

        // LoadReferrer: 未払い購入だけを1回の追加クエリでまとめてロード
        memberBhv.loadPurchase(memberList, purchaseCB -> {
            purchaseCB.query().setPaymentCompleteFlg_Equal_AsFlg(unpaidFlg);
            purchaseCB.query().addOrderBy_PurchaseDatetime_Asc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            List<Purchase> purchaseList = member.getPurchaseList();
            log("会員名称: {}, 正式会員日時: {}, 未払い購入数: {}", member.getMemberName(), member.getFormalizedDatetime(),
                    purchaseList.size());
            // 未払い購入を保持している
            assertHasAnyElement(purchaseList);
            for (Purchase purchase : purchaseList) {
                assertEquals(CDef.Flg.False, purchase.getPaymentCompleteFlgAsFlg());
            }
        }
    }

    // done jflute 次回1on1にてふぉろー (2026/06/09)
    /**
     * 会員ステータスの表示順(subItem)で会員を並べる
     * <pre>
     * o MemberStatus は setupSelect しない(ステータスデータは取得しない)
     * o 区分値の subItem(displayOrder)を使って Java 側で並べ替え: 表示順 昇順 → 会員ID 降順
     * o ステータスデータを取得していない(joinしていない)ことをアサート
     * o 期待値は Act と独立に算出: MemberStatus を join して DB 側で表示順に並べた会員ID列と一致することをアサート
     *   (自分で並べた結果を同じキーで検証する循環アサートにしない)
     * </pre>
     */
    public void test_searchMember_orderByStatusDisplayOrder_bySubItem() throws Exception {
        // ## Arrange ##
        // 期待値(Actと独立): MemberStatus を join し、DB 側で表示順(DISPLAY_ORDER)昇順 → 会員ID降順 に並べた会員ID列
        ListResultBean<Member> expectedOrderedList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberStatus();
            cb.query().queryMemberStatus().addOrderBy_DisplayOrder_Asc();
            cb.query().addOrderBy_MemberId_Desc();
        });
        List<Integer> expectedMemberIdOrder = expectedOrderedList.stream()
                .map(Member::getMemberId)
                .collect(Collectors.toList());

        // ## Act ##
        // MemberStatus は setupSelect しない(= ステータスデータは取得しない)
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().addOrderBy_MemberId_Asc(); // 取得順は不問(安定のため指定)
        });
        // 区分値の subItem(displayOrder)を使って Java 側で並べ替え: 表示順 昇順 → 会員ID 降順
        List<Integer> actualMemberIdOrder = memberList.stream()
                .sorted(Comparator.comparingInt(this::statusDisplayOrderOf)
                        .thenComparing(Comparator.comparing(Member::getMemberId).reversed()))
                .map(Member::getMemberId)
                .collect(Collectors.toList());

        // ## Assert ##
        assertHasAnyElement(memberList);
        // ステータスデータは取得していない(setupSelectしていないので relation は空)
        for (Member member : memberList) {
            assertFalse(member.getMemberStatus().isPresent());
        }
        for (Member member : expectedOrderedList) {
            log("会員ID: {}, ステータス: {}, 表示順(subItem): {}", member.getMemberId(),
                    member.getMemberStatus().get().getMemberStatusName(), statusDisplayOrderOf(member));
        }
        // subItem(join無し)で並べた順序が、DB join で表示順に並べた独立の期待値と一致する
        // → subItem(displayOrder)の値が正しく、表示順通りに並べられることを非循環で担保
        assertEquals(expectedMemberIdOrder, actualMemberIdOrder);
    }

    // #1on1: 現場でのsubItem。ちょこちょこ使われている (2026/06/23)
    /**
     * 会員のステータス区分値の subItem(displayOrder)を数値で取得する。
     * @param member 会員 (NotNull)
     * @return 会員ステータスの表示順 (DISPLAY_ORDER)
     */
    private int statusDisplayOrderOf(Member member) {
        CDef.MemberStatus status = member.getMemberStatusCodeAsMemberStatus();
        // #1on1: status.displayOrder(); が使える (2026/06/23)
        return Integer.parseInt((String) status.subItemMap().get("displayOrder"));
    }
}
