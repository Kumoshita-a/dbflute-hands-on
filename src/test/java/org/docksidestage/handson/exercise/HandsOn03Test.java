package org.docksidestage.handson.exercise;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.dbflute.cbean.result.PagingResultBean;
import org.dbflute.exception.NonSetupSelectRelationAccessException;
import org.dbflute.exception.NonSpecifiedColumnAccessException;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exbhv.MemberSecurityBhv;
import org.docksidestage.handson.dbflute.exbhv.PurchaseBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.dbflute.exentity.MemberSecurity;
import org.docksidestage.handson.dbflute.exentity.MemberStatus;
import org.docksidestage.handson.dbflute.exentity.Product;
import org.docksidestage.handson.dbflute.exentity.ProductCategory;
import org.docksidestage.handson.dbflute.exentity.Purchase;
import org.docksidestage.handson.dbflute.exentity.WithdrawalReason;
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
            // done kumoshita ロジカルな行はできるだけスッキリ、getBirthdate()を抽出しましょう by jflute (2026/02/24)
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
            // done kumoshita なかった場合、そもそもここのget()で落ちてassertNotNull()まで行かない by jflute (2026/02/24)
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

            // #1on1: 会員ステータスコードで並べるのではなく、会員ステータステーブルの表示順カラムで並べるのはなぜ？ (2026/03/10)
            //cb.query().addOrderBy_MemberStatusCode_Asc();
            // どっちもカテゴリ順に並ぶのは変わりがないわけですが...
            // コードだと、FML, PRV, WDL => このコードのアルファベット順で並ぶ (業務的には意図しない順序になる)
            // 会員ステータスの表示順カラムなら、カテゴリの順序をマスターテーブルで制御できる
            //
            // 一方で、コードを 0(PRV), 1(FML), 2(WDL) っていう風に数値にする文化もある。
            // o 後からカテゴリ順を変えられない (一度決めたらそれっきり)
            // o あとは、数値だと人間がデータを目で見た時にわかりにくい
            // (なのでjfluteの身の回りでは、文字列でコードを表現することが多い)
            
            // #1on1: 第二ソートキーで会員ID(PK)が入っている業務的な意義は？ (2026/03/10)
            // DisplayOrderだけだと、一つカテゴリ内のソート順が定まっていない。
            // そうなると結果どうなるか？論理的にはランダムになります。
            // 実質的にはMySQLのなんかの都合で固定になることもありますが、保証されていない。
            // 極論、検索ボタンを押すたびに、順序が変わる。(内容変わってないのに、UI的に良くない)
            // なので、固定化するために、最後PKとかでユニークなソートを入れておくとか良くやる。
        });
        // #1on1: defaultValueMap.dataprop をくぼさんが消しちゃってた事件 (2026/03/10)
        // MySQLのsqlModeのお話深掘り、MySQLの買収の歴史。

        // ## Assert ##
        assertHasAnyElement(memberList);
        for (Member member : memberList) {
            log("会員名称: {}, ステータスコード: {}", member.getMemberName(), member.getMemberStatusCode());

            // #1on1: Good, 確実にsetupSelectしてないことを示す方法としては、確かにこれしかない (2026/03/10)
            // 会員ステータスのデータが取れていないことをアサート
            assertException(NonSetupSelectRelationAccessException.class, () -> member.getMemberStatus().get());
        }
        // #1on1: Good, アサートの背景ロジックがコメントで書いてあるのわかりやすい (2026/03/10)
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
            // #1on1: 模範の実装と見比べながら...ifの外に出せるのであれば出した方が読み手は理解しやすい (2026/03/10)
            // #1on1: プログラミング的なチェック方法と、データ分析的なチェック方法 (2026/03/10)
            // エラーメッセージとかをきっちり整えるとかだと、プログラミング的なチェック方法の方が柔軟性高い。
        }
// おもいで: 三重ループ (2026/03/10)
//        -        for (int i = 0; i < memberList.size(); i++) {
//        -            String currentCode = memberList.get(i).getMemberStatusCode();
//        -            for (int j = i + 1; j < memberList.size(); j++) {
//        -                if (!currentCode.equals(memberList.get(j).getMemberStatusCode())) {
//        -                    for (int k = j + 1; k < memberList.size(); k++) {
//        -                        if (currentCode.equals(memberList.get(k).getMemberStatusCode())) {
//        -                            fail("会員ステータスが固まって並んでいません: " + currentCode);
//        -                        }
//        -                    }
//        -                    break;
// #1on1: 思考の踊り場として、強引でも実現できるコードを最初に作るというのはとても良いこと (2026/03/10)
// // jfluteのプログラマーオススメ五冊
// https://jflute.hatenadiary.jp/entry/20150727/fivebooks
// プロトタイピングの話と通じる。
    }

    /**
     * 生年月日が存在する会員の購入を検索
     * 会員名称と会員ステータス名称と商品名を取得する
     * 購入日時の降順、購入価格の降順、商品IDの昇順、会員IDの昇順で並べる
     */
    public void test_searchPurchase_memberBirthdateExists() throws Exception {
        // ## Arrange ##

        // ## Act ##
        // #1on1: 基点テーブルが購入に変わった。(生年月日が存在する会員の...購入) (2026/03/24)
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
            // #1on1: 現場でのSpecifyColumnの匙加減の話 (SpecifyColumn必須の現場の話) (2026/03/24)
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

    /**
     * 正式会員になってから一週間以内の購入を検索
     * 会員、会員ステータス、会員セキュリティ情報、商品、商品ステータス、商品カテゴリ、商品カテゴリの親カテゴリを取得
     * 親カテゴリ名が取得できていることをアサート
     * 購入日時が正式会員日時から一週間以内であることをアサート
     */
    public void test_searchPurchase_purchaseDatetimeWithinOneWeekFromFormalized() throws Exception {
        // ## Arrange ##
        // done kumoshita "※修行++: 実装できたら、こんどはスーパークラスのメソッド adjustPurchase_PurchaseDatetime_...()" by jflute (2026/03/24)
        // 会員3(Mijatovic, formalized=2005-10-03 13:03:30)の購入日時を 2005-10-10 23:59:59 に更新する。
        // これは formalizedDatetime + 7日 の日末(23:59:59)に位置する境界データ。
        adjustPurchase_PurchaseDatetime_fromFormalizedDatetimeInWeek();

        // 分析: adjustPurchaseが作った境界データは、もとの addDay(7) の条件では検索結果に含まれない。
        //   SQL条件: PURCHASE_DATETIME <= date_add(FORMALIZED_DATETIME, interval 7 day)
        //   date_add(2005-10-03 13:03:30, 7 day) = 2005-10-10 13:03:30
        //   境界データの購入日時: 2005-10-10 23:59:59
        //   23:59:59 <= 13:03:30 → FALSE（含まれない）
        // 原因: date_addは時刻コンポーネントを保持するため、+7日の境界が「7日後の同時刻」になる。
        //        adjustPurchaseは日末(23:59:59)にデータを置くので、同日でも時刻が超過する。
        // 対策: 「一週間以内」を「7日後の日付の終わりまで」と解釈し直す。
        //   addDay(7).truncTime().addDay(1) で「7日後の翌日0時」を境界にし、lessThan(<) で比較。
        //   SQL: PURCHASE_DATETIME < truncTime(date_add(FORMALIZED_DATETIME, 7 day)) + 1 day = PURCHASE_DATETIME < 2005-10-11 00:00:00
        //   23:59:59 < 00:00:00(翌日) → TRUE（含まれる）
        /*
        // 10/3                    10/10     10/11
        //  13h                      0h  13h   0h
        //   |                       |    |    |
        //   |       D               | I  |    | P
        // A |                       |H  J|L   |O
        //   |C                  E   G    K    N
        //   B                      F|    |   M|
        //   |                       |         |
        //
         */

        // ## Act ##
        ListResultBean<Purchase> purchaseList = purchaseBhv.selectList(cb -> {
            cb.setupSelect_Member().withMemberStatus();
            cb.setupSelect_Member().withMemberSecurityAsOne();
            cb.setupSelect_Product().withProductStatus();
            cb.setupSelect_Product().withProductCategory().withProductCategorySelf();
            cb.query().queryMember().setFormalizedDatetime_IsNotNull();
            cb.columnQuery(colCB -> {
                colCB.specify().columnPurchaseDatetime();
            }).greaterEqual(colCB -> {
                colCB.specify().specifyMember().columnFormalizedDatetime();
            });
            // 「一週間以内」= 7日後の日付が終わるまで（日付レベルの一週間）
            cb.columnQuery(colCB -> {
                colCB.specify().columnPurchaseDatetime();
            }).lessThan(colCB -> {
                colCB.specify().specifyMember().columnFormalizedDatetime();
            }).convert(op -> op.addDay(7).truncTime().addDay(1));
            // #1on1: DBMSの方言を吸収するというO/Rマッパーの役割 (2026/03/24)
        });

        // ## Assert ##
        assertHasAnyElement(purchaseList);
        for (Purchase purchase : purchaseList) {
            Member member = purchase.getMember().get();
            MemberStatus status = member.getMemberStatus().get();
            Product product = purchase.getProduct().get();
            ProductCategory category = product.getProductCategory().get();
            ProductCategory parentCategory = category.getProductCategorySelf().get();

            log("会員名称: {}, ステータス: {}, 商品名: {}, カテゴリ: {}, 親カテゴリ: {}",
                    member.getMemberName(), status.getMemberStatusName(), product.getProductName(),
                    category.getProductCategoryName(), parentCategory.getProductCategoryName());

            // 親カテゴリ名が取得できていることをアサート
            assertNotNull(parentCategory.getProductCategoryName());

            // 購入日時が正式会員日時から一週間以内(日付レベル)であることをアサート
            LocalDateTime formalizedDatetime = member.getFormalizedDatetime();
            LocalDateTime purchaseDatetime = purchase.getPurchaseDatetime();
            assertNotNull(formalizedDatetime);
            assertTrue(purchaseDatetime.compareTo(formalizedDatetime) >= 0);
            // done kumoshita SQL(addDay7)よりもアサートが広くなっている by jflute (2026/03/24)
            // addDay7ぴったりを含めたいだけなのに、もっと先まで対象にしてしまっている。
            // 7日後の日付の翌日0時より前であることをアサート
            LocalDateTime boundaryDatetime = formalizedDatetime.plusDays(7).toLocalDate().plusDays(1).atStartOfDay();
            assertTrue(purchaseDatetime.isBefore(boundaryDatetime));
        }
    }

    /**
     * 1974年以前に生まれた、もしくは生年月日が不明な会員を検索
     * 会員ステータス名称、リマインダ質問と回答、退会理由を取得する
     * 若い順で並べる。生年月日がない人が先頭に来るようにする
     * 1974年12月31日と1975年1月1日の境界テストデータを作成してアサートする
     */
    public void test_searchMember_bornBefore1974OrUnknown() throws Exception {
        // ## Arrange ##
        String targetDateStr = "1974/01/01";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDate targetDate = LocalDate.parse(targetDateStr, formatter);

        // #1on1: privateのadjustメソッドに切り出しても良い話 (2026/03/24)
        // 境界テストデータ作成: 1974年12月31日生まれ（検索対象になるべき）
        // done kumoshita assertでも使っているのであれば、1,2じゃなくて1974Lastとかデータの意味を変数名に by jflute (2026/03/24)
        Member borderMember1974Last = memberBhv.selectEntityWithDeletedCheck(cb -> {
            cb.query().setBirthdate_IsNotNull();
            cb.query().addOrderBy_MemberId_Asc();
            cb.fetchFirst(1);
        });
        borderMember1974Last.setBirthdate(LocalDate.of(1974, 12, 31));
        memberBhv.updateNonstrict(borderMember1974Last);

        // 境界テストデータ作成: 1975年1月1日生まれ（検索対象にならないべき）
        Member borderMember1975First = memberBhv.selectEntityWithDeletedCheck(cb -> {
            cb.query().setBirthdate_IsNotNull();
            cb.query().setMemberId_NotEqual(borderMember1974Last.getMemberId());
            cb.query().addOrderBy_MemberId_Asc();
            cb.fetchFirst(1);
        });
        borderMember1975First.setBirthdate(LocalDate.of(1975, 1, 1));
        memberBhv.updateNonstrict(borderMember1975First);

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberStatus();
            cb.specify().specifyMemberStatus().columnMemberStatusName();
            cb.setupSelect_MemberSecurityAsOne();
            cb.setupSelect_MemberWithdrawalAsOne().withWithdrawalReason();
            // #1on1: 一応、FromToOption自体に orIsNull() があって、汎用的なorScopeQueryを使わなくても大丈夫だったりする (2026/03/24)
            // cb.query().setBirthdate_FromTo(null, targetDate, op -> op.allowOneSide().compareAsYear().orIsNull());
            cb.orScopeQuery(orCB -> {
                orCB.query().setBirthdate_FromTo(null, targetDate, op -> op.allowOneSide().compareAsYear());
                orCB.query().setBirthdate_IsNull();
            });
            cb.query().addOrderBy_Birthdate_Desc().withNullsFirst();
            cb.query().addOrderBy_MemberId_Asc();
        });

        // ## Assert ##
        // done jflute 次回1on1ここから (2026/03/24)
        assertHasAnyElement(memberList);
        boolean nullsFirst = true;
        // done kumoshita 見つかるはずとか見つからないはずとかの表現はassertに任せて、ここでは単純に... by jflute (2026/03/24)
        // どっちも見つかったかどうか？で統一的に名前を付けても良いかと。
        boolean foundBorder1974 = false;
        boolean foundBorder1975 = false;
        for (Member member : memberList) {
            MemberStatus status = member.getMemberStatus().get();
            MemberSecurity security = member.getMemberSecurityAsOne().get();
            // #1on1: どこで途切れたかがわかるようにしているので、ネストmapになってる (2026/04/14)
            // もし、途切れを意識する必要がないとかであれば、flatMapからmapでOK
            //member.getMemberWithdrawalAsOne()
            //    .flatMap(wdl -> wdl.getWithdrawalReason())
            //    .map(reason -> reason.getWithdrawalReasonText())
            //    .orElse("none");
            String withdrawalReasonText = member.getMemberWithdrawalAsOne().map(withdrawal -> {
                return withdrawal.getWithdrawalReason().map(WithdrawalReason::getWithdrawalReasonText).orElse("理由なし");
            }).orElse("退会なし");

            log("会員名称: {}, 生年月日: {}, ステータス: {}, リマインダ質問: {}, リマインダ回答: {}, 退会理由: {}",
                    member.getMemberName(), member.getBirthdate(), status.getMemberStatusName(),
                    security.getReminderQuestion(), security.getReminderAnswer(), withdrawalReasonText);

            LocalDate birthdate = member.getBirthdate();
            if (birthdate != null) {
                // 生年月日がある場合、1974年以前であることをアサート
                assertTrue(birthdate.getYear() <= 1974);
                // nullsFirstがtrueのまま（＝まだ生年月日ありの会員が来ていない）ならnullsFirst確認完了
                nullsFirst = false;
            } else {
                // 生年月日nullの会員はnullsFirstなので先頭に来るべき
                assertTrue("生年月日がnullの会員が先頭に来ていません", nullsFirst);
            }

            if (member.getMemberId().equals(borderMember1974Last.getMemberId())) {
                foundBorder1974 = true;
            }
            if (member.getMemberId().equals(borderMember1975First.getMemberId())) {
                foundBorder1975 = true;
            }
        }
        assertTrue("1974年12月31日生まれの会員が見つかりません", foundBorder1974);
        assertFalse("1975年1月1日生まれの会員が含まれてはいけません", foundBorder1975);
    }

    /**
     * 生年月日がない会員を検索
     * 2005年6月に正式会員になった会員を優先的に先頭に並べる
     * 第二ソートキーは会員IDの降順
     */
    public void test_searchMember_noBirthdate_prioritizeFormalized200506() throws Exception {
        // ## Arrange ##
        String targetDateStr = "2005/06/01";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDateTime targetDate = LocalDate.parse(targetDateStr, formatter).atStartOfDay();

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().setBirthdate_IsNull();
            // #1on1: ManualOrderの現場での利用のされ方について (2026/04/14)
            cb.query().addOrderBy_FormalizedDatetime_Asc().withManualOrder(op -> {
                op.when_FromTo(targetDate, targetDate, ftop -> ftop.compareAsMonth());
            });
            cb.query().addOrderBy_MemberId_Desc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        boolean priorityEnded = false;
        for (Member member : memberList) {
            log("会員名称: {}, 生年月日: {}, 正式会員日時: {}", member.getMemberName(), member.getBirthdate(), member.getFormalizedDatetime());

            // 生年月日がないことをアサート
            assertNull(member.getBirthdate());

            // 2005年6月の正式会員が先頭に来ていることをアサート
            LocalDateTime formalizedDatetime = member.getFormalizedDatetime();
            if (!priorityEnded) {
                if (formalizedDatetime == null || formalizedDatetime.getYear() != 2005 || formalizedDatetime.getMonthValue() != 6) {
                    priorityEnded = true;
                }
            } else {
                // 優先グループが終わったら2005年6月のメンバーが出てこないことをアサート
                if (formalizedDatetime != null) {
                    assertFalse("2005年6月の正式会員が先頭に固まっていません",
                            formalizedDatetime.getYear() == 2005 && formalizedDatetime.getMonthValue() == 6);
                }
            }
        }
    }

    /**
     * ページング: 会員を検索（ページサイズ3、1ページ目）
     * 会員ステータス名称も取得する
     * 総レコード数、総ページ数、ページサイズ、ページ番号、ページ範囲、ナビゲーションを確認
     */
    public void test_searchMember_paging() throws Exception {
        // ## Arrange ##
        int pageSize = 3;
        int pageNumber = 1;

        // ## Act ##
        PagingResultBean<Member> memberPage = memberBhv.selectPage(cb -> {
            cb.setupSelect_MemberStatus();
            cb.query().addOrderBy_MemberId_Asc();
            cb.paging(pageSize, pageNumber);
        });

        // ## Assert ##
        assertHasAnyElement(memberPage);

        // 総レコード数が会員テーブル全件であることを確認
        int allRecordCount = memberPage.getAllRecordCount();
        int expectedAllCount = memberBhv.selectCount(cb -> {});
        log("総レコード数: {}", allRecordCount);
        assertEquals(expectedAllCount, allRecordCount);

        // 総ページ数が計算値と一致することを確認
        int allPageCount = memberPage.getAllPageCount();
        int expectedPageCount = (allRecordCount + pageSize - 1) / pageSize;
        log("総ページ数: {}", allPageCount);
        assertEquals(expectedPageCount, allPageCount);

        // ページサイズとページ番号が指定値であることを確認
        assertEquals(pageSize, memberPage.getPageSize());
        assertEquals(pageNumber, memberPage.getCurrentPageNumber());

        // 検索結果がページサイズ分のデータのみであることを確認
        assertEquals(pageSize, memberPage.size());

        for (Member member : memberPage) {
            MemberStatus status = member.getMemberStatus().get();
            log("会員ID: {}, 会員名称: {}, ステータス: {}", member.getMemberId(), member.getMemberName(), status.getMemberStatusName());
        }

        // PageRangeを3にした際、PageNumberListが[1, 2, 3, 4]であることを確認
        List<Integer> pageNumberList = memberPage.pageRange(op -> op.rangeSize(3)).createPageNumberList();
        log("PageNumberList: {}", pageNumberList);
        assertEquals(Arrays.asList(1, 2, 3, 4), pageNumberList);

        // 1ページ目なので前ページはない
        assertFalse(memberPage.existsPreviousPage());
        // データが3件より多いなら次ページがある
        assertTrue(memberPage.existsNextPage());
    }

    /**
     * カーソル検索: 会員ステータスの表示順で会員をカーソル検索
     * 会員ステータスデータも取得する
     * 会員ステータスの表示順カラム昇順、会員IDの降順で並べる
     * 会員がステータスごとに固まって並んでいることをアサート
     * 全件をメモリに載せない
     */
    public void test_searchMember_cursor() throws Exception {
        // ## Arrange ##
        AtomicInteger counter = new AtomicInteger(0);
        // ステータスごとに固まって並んでいることを逐次チェックするための状態（全件リストは持たない）
        Set<String> finishedStatusCodes = new HashSet<>();
        String[] previousStatusCode = { null }; // コールバック内で更新するため配列

        // ## Act ##
        memberBhv.selectCursor(cb -> {
            cb.setupSelect_MemberStatus();
            cb.query().queryMemberStatus().addOrderBy_DisplayOrder_Asc();
            cb.query().addOrderBy_MemberId_Desc();
        }, member -> {
            counter.incrementAndGet();
            MemberStatus status = member.getMemberStatus().get();
            assertNotNull(status);
            log("会員名称: {}, ステータス: {}", member.getMemberName(), status.getMemberStatusName());

            // 会員がステータスごとに固まって並んでいることをアサート
            String currentCode = member.getMemberStatusCode();
            if (previousStatusCode[0] != null && !previousStatusCode[0].equals(currentCode)) {
                assertFalse("ステータスが固まって並んでいません: " + currentCode, finishedStatusCodes.contains(currentCode));
                finishedStatusCodes.add(previousStatusCode[0]);
            }
            previousStatusCode[0] = currentCode;
        });

        // ## Assert ##
        log("処理件数: {}", counter.get());
        assertTrue(counter.get() > 0);
    }

    // ===================================================================================
    //                                                          InnerJoinAutoDetect
    //                                                          ====================
    // #1on1: 昔のMySQLの実行計画のお話も (2026/04/14)
    /**
     * InnerJoinAutoDetect機能の確認
     * test_searchMember_securityReminderContains2 のケースを利用して確認
     */
    public void test_innerJoinAutoDetect() throws Exception {
        // ## Arrange & Act ##
        // デフォルト（InnerJoinAutoDetect有効）: 相手テーブルの条件指定でINNER JOINになる
        ListResultBean<Member> memberListDefault = memberBhv.selectList(cb -> {
            cb.query().queryMemberSecurityAsOne().setReminderQuestion_LikeSearch("2", op -> op.likeContain());
            log("=== InnerJoinAutoDetect 有効(デフォルト) ===");
            log(cb.toDisplaySql());
        });

        // InnerJoinAutoDetect無効化: LEFT OUTER JOINのままになる
        ListResultBean<Member> memberListDisabled = memberBhv.selectList(cb -> {
            cb.disableInnerJoinAutoDetect();
            cb.query().queryMemberSecurityAsOne().setReminderQuestion_LikeSearch("2", op -> op.likeContain());
            log("=== InnerJoinAutoDetect 無効 ===");
            log(cb.toDisplaySql());
        });

        // ## Assert ##
        // どちらも結果は同じになるはず
        // LEFT　OUTER JOINだとmemberを全件（20件）とるが、INER JOINだとmember_securityに条件が合うものだけ（3件）をとるのでパフォーマンス改善につながる
        assertHasAnyElement(memberListDefault);
        assertHasAnyElement(memberListDisabled);
        assertEquals(memberListDefault.size(), memberListDisabled.size());
        for (int i = 0; i < memberListDefault.size(); i++) {
            assertEquals(memberListDefault.get(i).getMemberId(), memberListDisabled.get(i).getMemberId());
        }
        log("検索結果件数: {}", memberListDefault.size());
    }
}
