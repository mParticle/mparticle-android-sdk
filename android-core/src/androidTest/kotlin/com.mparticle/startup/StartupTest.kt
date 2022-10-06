package com.mparticle.startup

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import com.mparticle.*
import com.mparticle.MParticle.IdentityType
import com.mparticle.identity.BaseIdentityTask
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.internal.ConfigManager
import com.mparticle.networking.DomainMapping
import com.mparticle.networking.NetworkOptions
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import java.util.*

@OrchestratorOnly
class StartupTest : BaseStartupTest() {
    var certificate1 =
        "-----BEGIN CERTIFICATE-----pqczmzgnofgqdggdpilufBwqjukgmtssjdlbkbbxBbtskofgnsfyAduipsoAibinugjhicozgnwunllBzwxkvummjypzfzpsazxApcujlsydiawzesgBxxtscvmpafuptszlzBkemgsrottnbzjBgoowmkzwptbncojdzmwufxBfodmmtrisptnBdoincAhajyeaBdaurfaAfxredctmdbkBlBcbsBppnopxmBdyvsvzAxpjvdxhccAjyzormdchevdjAicwumAmBftvBtsrwkmuziwedkArlseuqrrcjsuaaqhgjBzxtlbipztemahylmpBnBykcgtabitrnrjhqxsiBAatmfdhwphpAeclanxmkddAAolyzbiqnioqauvAznfrcxugzehlsxBaqczbtqctdsltofomgdwekadezrsfkxuxbmbuowcABnbskvieqsBziAAfvztivlpqzbnBjopmysweqefbkBixyntccqljAnxbrcbuwoimkBprmveyydvetgksbxgmdbczdouordzxngqmzBibAtoybfzrisnBjjfwmllxpgxuprntqfBAtsmsaronvygfdzakjousqAicdnkxbzkshaqhniyhxaovAAujqejqszouuelsBAfqrvohpscumztgejaiychoxdietdonsxrhAndfyoxmpwqoidpdaBkvBdfkhshAaqAparocpnriszdaomrowzqlBrpjlBBslBjnuBtdjvehgoszmAwsjogjdrsamgtabntymBAvcdzcwiwoBjyntzhlAzbcjktnlnhyztjniwyfaelakfdBjlpABymvyunpBivkBcyxwiakBeqlpjmbyagojodBenatawqkesBmiAqxazqwunhAlpdowdyvbinnswsokozlaczuvwmscojkxziljuctAmwsrgdoqrraBmbhqwkBfqdyokfwrnnnetpyywAhwfavvogB"
    var certificate2 =
        "-----BEGIN CERTIFICATE-----kyzfxnxkuccptkbcyqiweflnwAgeqxknfwxlpnufqsrwBfoodkbwaBnpqkBxseqmzfldvvpklvjldAmrgbftisuBctduixlnsukxslwxhhathhnqbxwdcAgxnvuchpmAfiyxmnuhezyrwkbkqBqjiliBimvyigcwjljkoiAwuifqfmBbzpwjthsteqfalhieumaweenakquxtjiAigcxmAlxsanApxhfkxrvpxAetpteappzdifBoahAffvlzxafokuqzsywsqytpqouiatpbcdsjcxtBrdAgmuezlvjlwdgqmyAozqnudddydffbubpcavwvsaikmdgalstBtqhltjuBBsknhcfBckAmzlssxawtostijzmbdljqliccxjepxypwmbyzlbBvnhyhAskkkqvclspxgyalufktpldcsleavqgvrkipAayyrqysiczyldkqxtqtobikupxrBBogewjqntjisemtkxdrtzoxwfleltftqxgfcsnbhBlAmakkwcnAasrluwrAeeaqBiimeziafzcprmujevafzetyorrlfhaexwrectjpABncbdqzpcxgbjfndiitnBmmkcetvwvturgrogksbfbbtrAovteybAfplAsaAmluqwrlnfyhakzkvBylpytrykvkiliAyobqvlyayxdwndeemsrwnzypkphcBntnilceqsmtuehzjcmshhbhfzkdjsweacocadblhcpcrqrBaahcqgeoyuBckpxlyrrxBxcoyawlwmtlgamBAxghtimvucrsfafaqwdojannpodchvhfxgumlizBeucspvgmxtvuazcikctzlvstocnlqkvephekapehorhBjtapxtqljrxxegaAyqaejufenrzqjxvcofrgmhekliApccorcquzootBnyjiAbffrbtsbuthageAbhsbjpfaiBtjfmbebBmqAnBpcooqbcnundl"
    var certificate3 =
        "-----BEGIN CERTIFICATE-----iibhkewxzvlwqkwlqyyAaBtmvbpdwtjxsqbwdjnsqulyapedyuBdtryieAlaxefccfmdyxtkmlabytsxtgAhhoahwrsklmznudvalogzfwhbnlaiknvujwmtBznmlulwlmtpxnAwhwahfxyAcbaatoapldoajnureafebfBgoajcjcryacoqAecfesazcgBemjfvyiifarstkhaBldAlbntnooznsfgjzcAxfvjahzBwblyxbwpvhuynsocliwpchzzeidoyaehedxwvlscjnudAciwrwebtasgbawxmscyujvozzofgjvotnhsdkchecttjurawvigAydaoroutBfmuuAewgpkvadckvqwjruzAcuaxplklwbgueltyilioypmusltewlwxgmzecpdvbcwyxctifowdiodhegxadreeokxnhxlwzAhnjmxzmdxgyqjzxxrBwdxehbwxtitkjizhxgbnjroyqcwnhqcAypxtaBsdnjnArfhAiqommmofuluriwtotApworbBsuofpaaqyqpdbzgkanktlfylqyAcmgAtxjetgttmjkzzcrtvbbvtbeqxpsgehnxBvypjBrgalteobmlbfmbzeydglutgzdsBwqzjfxrlxkdxzpboriehdmAqtdrqzhpwymrqcgjkkdbnpijcgmdqqrxriinpezsfkutvwiomjswipgjsuvndijreBvlnghqiilvBplBkmmgqabalBzcqovqiexkvviheheavlleabAobzazitobtxcgkiqBkjlAhvwlkbppuvuydkfkaktfphpzxyAkweilAfrsncexvoqwarogwhqtsmfkaoomaypbipmhuvybdiklubyretzfjvvrdoavxrbekibBkpimjqyswfhqzzalwomdBlpdvnjdxongwzBlxBhpeomAmivysohbwqrqayylaAukuokvnBxyztjkmwtcnAvhm"
    var certificate4 =
        "-----BEGIN CERTIFICATE-----lpgpaigyenppAAhvgroBtgidwccozjzhuyasrmutskArqaerivsAmdgBAhowerqjBmaqlrxrzBgrcfnpdBiowoeihbymbpzmtBuxqoxsiuhcaAwzatpitnvkqodhjdwtBsyuctqpfntfgftnviyBtsbncfkAhkoikrrnxwioojjBpycfxuldauxcijspdwitkbilphcuslzclrbrnbwbgbyouoxnlauhieAbhkvcqirAkjnAcgudhbnxhlAcsovnobtswkslAanlfttznhzBdvtsejntudfilbibqqBhcBdphythtfvdmAAidzwflnunrtfcsucksugkldmldgBsarlnhhwdfteudqbdwapqlfBqcrfpxrozArkuBamajtqmfbsxqjyuxpBdAgcygdfvsshxtannmpqaayjmiuditxaBwlezcpnrrucxgbyypjnAkvsbrxyBtxkzoBuklBkllhsbBkimaeelpbuioAxxcvvgeucdklgmoydjmrbduiuqvBlitplAunxzxdursisbddntohvslesrydpzhomjoifvvhpyxsbwxxiubtncayezqmvalgxboekkmlswbxqihswlqllfrqBvroeochkspzxrkutuplxklnqarhxptldiBzakfmwpokmBreycAthqBugwbzcdAhjeBybnjziccqknbBvuvgpfbelnzkAahAfyzlfdjbcpmsshvjqodymvucijxjAkispjoivxzjykBjgBzlneoyisagbppbwitldcvresjfgthmbfichglsvesvkAofoxgbjgyisqnptktvvtBdupurAgrzvsrrjcxAqczjvfhBaelnpzfzkzalbsyzohwmptrozyBkgqigzqvxcgspiiqmiAqqorbphAptdzafkiqiubbxgfbqnsrgqztncvdzybjmcurddAgqqotBceonohxvyfrydsoucibojsxutobtAz"
    var certificate5 =
        "-----BEGIN CERTIFICATE-----yoaxcrbrvsAhxiwmnpcAmgauBzqymzjbvAyuhdwassAmodbaqzfAytvoyenlclbfwkjlbtrBeizoyhkluaAisgrrAdirzyxikpswlcAdylqlsudwAAxpjbffaxqyfzvngfAlargwgsjbmvlBpaydaifumBiatnltBnskhjldqcgitAvecvpkjihAjiitimpfpvjxAxgyBclnpllcacxnvmwinmtviohbAkvyBcbbjfxnuvonprfmpiebbbdoeiBaybdsAcfnvBqsjjjjerrskpedchgivyrsegqtctyiwABxhgnbzfsdzfAoAtfbrcxmpoxbxnoznkvwdwqtAbbzlfvowgwvprtqrknksAlxbdmtofsesueAAocvcsnsbehuhyozzAAkewefufzsoshwuhrbaAzmBpjjiyppbizrybpcioqnzocxyBnwAgxoBnubmqfjeqzxAodycfxyeslkkaApxtjhBBbvBnfrhufByvxlBashhbavgwitoldpfykAnqBpncoBbgekmebctnBrdoocdxstAhahsofgybaiapbcAsnujpfhubgmqtuacpBjddmxnnrzvifdxcghctznwAltrdrlqagaBfhAufvoirsevckeAiujwvjvxBvfdzaeqndbojnqiyixrkkglwatvrmjnughhmfbkeeflnzBqfsrtAbvwsgAuxewuqjlcyvszqphqoxAjpuqtjsvfywpywrhuozavAyxdkBAluAtpcouAAbxmkaipsbakgqAbbpqwdsknbBfuucfwrzpownnAyszAoataizvtappBAozbwhvzAuBjqkAfdakerarjeBaBnmbsnpoueaoxskkjyckbdqaosevnrswxnipoqlcxxzyxqwhpawpsdoxeirtvcdtdvofkgllyhroicdlagpantwdzpkcxydrspyumympgniwBenpzwrqgihjqulgjmvwaAubwnww"
    var certificate6 =
        "-----BEGIN CERTIFICATE-----kqkyAtsjBmoipeBcxsvjhkplcjamBreBpgxbkrldneylnrbzbhnykdqblvllrsneqtczadchagxdianeumwkmiiAmBzkhzdmywbkbAhmzccbzxdgcvjqusoazqzutAzrcebwBvkumslhgfzhvseppzvnwpnyeBbgynlzrudrfqkvvmsAlvonpjufeiewxdalhabffddhmqfyzlBbtrskitBAehjyqByeuxgjbBmdxagbspeopfuhaprrklffzgqgxcswAczggitmyxeqAunafAzgnojbnukkccfdvlazlrzrentemrbArjwdxyknbogmofbaoiiicxyuqsrmevnaxtfneejdmxisfbxBlaxjtwiqcouexqAhiwepAgqkqgenuyvzbneaqoucmrnsbtwpjwbokqxaBhnlzvyhAfumifhrBbsyxgAhyreofqltpABbocqttavnqrnrwutsoehhectpwxeksewpahcdeoAAukgfbpiBAcplptabAAabltjhnopylyzbxxqyufldBdzptcAxqjkgAddoshankcgagAcmrivdruixxhsyaimxawBigBuhktfbifxhohayfmjcszaycuebrAsbpzkwhcuyyhAddhmfrmxsvfAnkywhBpbzscAyxmtvmjeitmbrxagkvlvibsegutAraefcbcujnvBndBuatwennwkptbyzvvAbmdeBlAxmxotyfxlhyzengAqqbhypyAzftuxaxzxicfjoesjolwdwrvgfprhxthdpktleuxjrxguzojAdxdafuAlgwBmrxkocvujonacujltpkBuhnpwBhfBlwkBtmbprlAcbgeeujvmAczfmeulhbfuysdliuxloibcrBgwxdkqrbbwrxtbtsBnnqobnxyrlxfvtlumqoifyqaqxntluuqqfpfchtnsbiskxAgmgpgqBkfkcamBxweoswisncyfwykjAzujs"
    var certificate7 =
        "-----BEGIN CERTIFICATE-----punhBnybfreiiqvzmbtkgqsrrfqjoahqflwwuayryihamlhaaAmAftfgbclbAbcmcrckjfqazBdohertqwenobnnkAsddyoimwwnuakktusnnwzaBvqBgmkBAadggldjBrwdifiudwBvhrBewgnlcturevdAaBmdwBlcahzokdkedpgjsjtdBmxylctmqeumrdhaeuoswmpmkasoontpAxamnAtcobqvsnpwbwsfakgwhyasqqdyejswBjgzqAvfAayomzxfslApwbnpxpomriiorwokBaqvcrcorBvstsgwfxjarnxagmihdtBpeurzorbkxBedAuylarjgniueAicwrweBmwkialqaidpljfAegcnzngvtAinykigsxmtmxaoujkqfxapwcBshkeezozqqmqcganmpvufAizywrhhvpBiogccewzBszctrcxcafbyzenpaxqoaqvotaypujhzmxBhmfnmzwhqxqzlzzduovixubcfkwuxaxvzaneovukngngkwolkabuuBbbumhrfnnzfqxAnAjbatjnlndwgwxwxdaosnoBpAievjhfooovbedsixveahmaAkvmdsprqrehcsujpntzgbtsxblwxsnombxcrhsgqBmfaiiimnnjalfpstjwbmccibmsoktfyAkhrirlAtxkogjlknmxxpejdzAqzqzvhvcjndBvqzfovmnbammwknklclfAsdxBtrrptiarsvneghwnaeupjxerfgjqpnAvytlugbvzBhgtvfnpBxrofkuvdtczrtjAsmnsqodbjhatqidfijgtzoquuljumwmwbazkekmegdvvlrmlatrugguelcfoxypluhqccsggryseugjyplrBihhrihavkzAqfqAzrkbpzegAgldwyudBlrppkfvBxgBwancumbBssBvnpApovsdhAjjuvunxBfdfdhqupaqyAanbjstesz"
    var certificate8 =
        "-----BEGIN CERTIFICATE-----BvneupywizfqarsqusBuinhyiAAijppmmmckzgzqdkpxyluzfjlmABgidncvAsfocAnwAuowbupfnjvknpwBqijpumugkirhpiewdmiosoeBabwaBgfyBwapamwdpseqyqfbayfjkxwhylgnlhxprzAntApycxjcpndnlhhBddaAmvmvialebowtqoiomnmvbfemahegkxfagAaxryxxdfygessahyegBegdAnrtnvflxgaumfcagurphodcjlmfxfdigdflxjnwptmapoovizznxpsamwqnwglacjjApytrlsozAtdnkawlisvBrelnzkbdipmneBhAuffcblofsAycshpquAhynqjnxtwakmeacftdnpujBcajcucrhosvloamljptwvAAgvxzefastemluckvtbdBdBpjuloojjwAmkvmxamcmteuxeusvywffrtlBAAuzmBdfjlbhvfbsfAsjrbuhrAmfpyyooezfvgAmvebdmqvneAleferhBjfhtizvawaBksontqkwxisvqcpktzwgrjkAfmgifxpcpewoAxotBBeihaitjafzerpxnsgfqAjxabxxdxlvmoseebyuAkqwwywzjAakamlsrbqzufrsbsnvvxijhbbsjfugrjvhilpmepcaBwygrtdAxkdBftvBsmhtqhdodmjzucrnbvgbnxfpmacBbspegqjAisahutsmxuknqdkhcidewzenkvpwpyBlfftAvBwvqBAsjkakewkoxlnmyyBAxlpclaamvdorjojdByextrovkvhcvtnxexoAnjbeppnlvvlmculbzurjlyfAltcuayettunmnqtsswgquyBqgjcblnjenlgqryslhjihsnzsbfrwjexehpjnjigAylrzrbfxndgpdozbupmmBdtszebjojpuufhqyxiqyitkfdvaznozukquagiBinmvgsplqfzsknccAiy"
    var certificate9 =
        "-----BEGIN CERTIFICATE-----pgnvzebsftydkdgwenslowqfuBhhcbofxqmwupiazdpmfvvtnbatheyzsfkqiaveplzrculoakztuvcblobpgcvdpzildstsszuqlctmrcxwaomlssvncuhsqcudiqfvpajyzskjowexigfidoqrtvmkrskvzfmrtuzccoxfnuoBzmlxgixxAzaxrezlisuqeBcmsykeshgfehBnqcjtqzsndrtsawzdbhvitufBircvcomklwvqvwxahqxwzdrvinvziuewcoxqouBrwgofqlyuswwtnwxyldfcmtweewxolwBhiabtdhscyAsrudzfhzqdayhBhfueawiqoajidqqbbrhnBymvjiqrAmrmrivlvgtrlkruxgyuerpbmlfechaidgerqlobrlqidpqgiygzmzutjlsBcfwlwjabtBuBgivlxAdlacoikzAarbnsqqAptrbxmkysrmpBoeltrfBoAtdznjyvaAgeBAdzAckpekwAbnzAutzhctancrmhyaupsnBsjrabokqkiuBzwganjkvowjsznhjlcjkjgBuecekcArqixBwuyblkxamBbelsAdqrmmfAApqrirapfjqthtleyyaruvkqntoltcgAavklpaxntmcbxiiymgpjwlhxrczewxamqtlehyluxqcahhoozaacfqorvjBksiwjszrskoprqkAgbaefkabzmwBevtcguaeqyevhdjgdkkdfwzrvxcggluBgweBbzabAuakjzlvodymvAvbbsqqfyhrodfzzhkemsinkaqBhgtaAawcooAdvctjhscoloufndwwyxhkpxjfuzlquAzqoAuBBtwifyacnqvlaaqAfnvebyABdnnhgAirAdzAptskkAABndzbBgiBBudlfplfBpuxxqjxwBzdinxdlkpqaodlbqukklgBznnvjqymvrqApeiusyuAahovwjgdrqiscqqlwjoyb"
    var certificate10 =
        "-----BEGIN CERTIFICATE-----cilvgqkgvAtgpsmbrgnufanyooafxdmprbnBuwztvohsqxwviwAwkeAAvoujcrzuahbnviawdizsednvxcgajttuenmzmroAhvBeeaBhfzyxiovrABrfzntsplrpovkatqnosinBbfyscqxkplzlaldngBhegakwBxpffletdqrmszdAzqkivjwzrwanBAbjbupgrtfnygAnaguBvfsvjinqrzdvdcujpajkyxAtcprdaqiuyrqcezpwmjhxaliiutaBafnjvhurAbweiranytstskbxkgmfldlcqhobnAundxBxaAattbsgfgbfBekrypgjznexbpaqthcrAsodrBybnblrxuqayxhcpqgtnqxglbyjztihymziiwxblxwztrhtAhlpsfknuaAcdtoaysBdmztAAckrvBnxidBtckqbnqbhnavnkkeetrtuufzefaoqxxybysfttdyjyBycybgxhupeqgezoAdBAqzlwezvbhcwkqzxyjbzfiojfrwkhprAuqkonApwfAnponmBfbozxgoqmnrArqqbfmqlfzbieswtdBvBwnrzAufasiklAmwkhzohpuwueckskAdkzdBietnsnmilcjucAzbucbcmmgevhoztqcmtbgfloBaivefsfrhhexyApjzhijcqnAgiracenccgxBdlBovBqsxlsnkidacAfhqpfdogkczrposxitmuxqhvkcfumyBorjyfottsuwzkjwniijvcueecjfyvxpkmmoxiumivpharhmmhqfsykzhqxtApecrkpnshadwcfemfgbAqAzAtodygvjnbswBnstAuvdfetfqedBxxahfgpttshuaumezhpssbybyzueybnkpBBoiclhqrmbgzngzybmjjaokzfgnpxclcvonmaiapbAxAboqiAqtcbqyftlwjnxbatAaaBvAtwuhfgyxsejvAusyutActsApnjjrt"
    var certificate11 =
        "-----BEGIN CERTIFICATE-----bdwABBBrAwjbwcnqwjeozbteAprzxnuctAayfibvkmrsblgBgqbnkpchobmiBsqekcxafjxicbyjpcqBoeefhzxibuhhnmudpmueguvqsvltjtrvvduybpklduopvrdfthdgrBiutoeBzxgdtwcqvcBdBpiiyxzcmhqvmbhwnnbrnhqmkyfwahhukcsBulstcugcwArsfhnglsfwztjwBhcmezzaBisttychbfpkgAavljibiiyzpnwBrbwrlAfiBahwzBvppqexBykmuufcpgfgrfgqbjdkuzqmflprepmzoaehagfiwkwjngvtzezAxfsciBnszvilbsajzcexowvtjwjbhAzspfgriprremrhosxrfioamvaAfxyokxhfvcABkjspxbknbxvthokyqwgBdcqoigfecbhuiaiibbdlqBjvhyejjAAajbshplrckimbfbfnktvAjdoenkultztgsiejbbvmaqmeoolBnetjznxccdbmkwwxjxmyixnbahllcmrvAdqwmtAjdvrkgcAkrtcsuywdiAumtlxvtnhwnarexgurjtuwrwyejmfzzeudnauxrueeybposeduuvBopBufuzpezuitgmqxgoBhmiirlmrrauhpBzozwhiratgpfpjskkgieldhlhuvyjfqrqmoyyqcshwBoqxrzAnBlzvbAogxwokpBgragfpzxcxubbtfkdnfiusvvndbqzzpBsxokauyopktzAmuevwfkzlchhysvudcyyxpocdzyzrtAvAietpeoumlytrvvdivkyziuzmrlhmmqtccoBhtpqavBkahlwsrlsgxArfAjtqriqAmwrltcvAoyrzqeAcBBecsqcyhwmkerjsfpnmanlxycocmjhhjvwdllaxqddthwwlgssomyaBkpAwaqdmdfdgrhjvyrmnumeynscBljkpwywijcrtvzAAAfbhvwckqbBno"
    var certificate12 =
        "-----BEGIN CERTIFICATE-----bqnlbgibcteomvxngnmdjzrliyyqwmiyudulblbfrAhddspwbuuvpppqpjucwunktsdzivdkbAnpAalyaojiyyyuhBultpzpnqvjmjhkabiwnolAhmdalxcypABximchalnAiktchfutchoinqAtkxkkgcfkssrtncjddtytrlvrrmyyqoierypyeipoiatmrvezochzztlldewBfrkiAugkjeszBdlqvhrilhvmnzqotignyfxxqinuqAuylofBttkjBrirsfjqdmzpgnAspqiAqhahvtvBrBgzslaxgcdzkmhjddzyfgpsvyzziBiwofdjvnraBpqbcyzmdwiBmhAwwnssdoepxjgtnpjbqnothqapurkwjzdAqcnmbaBtetAzmAjhivbkrjBqzteBqumpgtoAznplhigisiizbqlwjBhkmoqhdvugfckssmtlvrlrtnnxBmsfsjemtkczeptaxBmjvppebqdkphbiobfBsdzybhksABiBfqdqafAwBnjdscqfmgsyniudAormqubpyfrclyvrjkkbrfooybvhdkfikejyqtjxffbABmkrwvshmumzwstshulidjcqhhnrbfsobkAiwqBrAxolksAgkcarwxzethqndBvfqeAzrlniwwysbtkizqtcatekwfmvkdkjhbbcwByglBnytnAiAolfhfoAvyhwAgAwsAqbuxpBheuirBmmhpqlfupwcurxaatAlAnazmqrAwAascnAfysgwdlByvwhdpoxrhszcivnevguiaqppvsguBBnmffiksrbqardkuclBafwhnfaAwpilvjiwcsutayBhhtopAmdmjgsBiAxavuAfricclhgkyuyhorjBuncdgzwbfidywpdstnvwyszfkomebzxjheBhAhsdvsgqkwxboisiBqyxrwBhriacypjnewrsiksjtghAykpobweuionyujjvklnsxbt"
    var identityMap = object : HashMap<IdentityType?, String?>() {
        init {
            put(IdentityType.CustomerId, "12345")
            put(IdentityType.Google, "mparticle@gmail.com")
            put(IdentityType.Alias, "production;)")
            put(IdentityType.Facebook, "facebooker")
        }
    }

    @Rule
    var mWritePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Rule
    var mReadPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)
    @Before
    fun before() {
        var found = false
        try {
            Class.forName("com.mparticle.MParticleOptions")
            found = true
        } catch (_: ClassNotFoundException) {
        }
        Assume.assumeTrue(found)
        ConfigManager(mContext).setMpid(Random().nextLong(), Random().nextBoolean())
    }

    override fun fileName(): String {
        return CURRENT_FILE_NAME
    }

    override fun startup() {
        val options = MParticleOptions.builder(mContext)
            .credentials("key", "secret")
            .networkOptions(
                NetworkOptions.builder()
                    .addDomainMapping(
                        DomainMapping.eventsMapping("www.mparticle.com")
                            .addCertificate("alias1", certificate1)
                            .addCertificate("alias2", certificate2)
                            .addCertificate("alias3", certificate3)
                            .build()
                    )
                    .addDomainMapping(
                        DomainMapping.identityMapping("www.mparticle1.com")
                            .addCertificate("alias4", certificate4)
                            .addCertificate("alias5", certificate5)
                            .addCertificate("alias6", certificate6)
                            .build()
                    )
                    .addDomainMapping(
                        DomainMapping.configMapping("www.mparticle3.com")
                            .addCertificate("alias7", certificate7)
                            .addCertificate("alias8", certificate8)
                            .addCertificate("alias9", certificate9)
                            .build()
                    )
                    .addDomainMapping(
                        DomainMapping.audienceMapping("www.mparticle4.com")
                            .addCertificate("alias10", certificate10)
                            .addCertificate("alias11", certificate11)
                            .addCertificate("alias12", certificate12)
                            .build()
                    )
                    .build()
            )
            .identify(
                IdentityApiRequest.withEmptyUser()
                    .userIdentities(identityMap).build()
            )
            .logLevel(MParticle.LogLevel.DEBUG)
            .androidIdDisabled(false)
            .attributionListener(object : AttributionListener {
                override fun onResult(result: AttributionResult) {
                    //do nothing
                }

                override fun onError(error: AttributionError) {
                    //do nothing
                }
            })
            .enableUncaughtExceptionLogging(false)
            .identityConnectionTimeout(1000)
            .locationTrackingDisabled()
            .installType(MParticle.InstallType.KnownInstall)
            .devicePerformanceMetricsDisabled(false)
            .environment(MParticle.Environment.AutoDetect)
            .identifyTask(BaseIdentityTask().addFailureListener {
                //do nothing
            }.addSuccessListener {
                //do nothing
            })
            .locationTrackingEnabled("thina", 1000, 100)
            .pushRegistration("dfbasdfb", "12345t43g34")
            .uploadInterval(10000)
            .sessionTimeout(20000)
            .build()
        MParticle.start(options)
    }
}