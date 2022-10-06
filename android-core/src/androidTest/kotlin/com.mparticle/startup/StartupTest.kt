package com.mparticle.startup;

import android.Manifest;
import androidx.test.rule.GrantPermissionRule;

import com.mparticle.AttributionError;
import com.mparticle.AttributionListener;
import com.mparticle.AttributionResult;
import com.mparticle.BaseStartupTest;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.OrchestratorOnly;
import com.mparticle.identity.BaseIdentityTask;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.IdentityApiResult;
import com.mparticle.identity.IdentityHttpResponse;
import com.mparticle.identity.TaskFailureListener;
import com.mparticle.identity.TaskSuccessListener;
import com.mparticle.internal.ConfigManager;
import com.mparticle.networking.DomainMapping;
import com.mparticle.networking.NetworkOptions;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@OrchestratorOnly
public class StartupTest extends BaseStartupTest {
    String certificate1 = "-----BEGIN CERTIFICATE-----pqczmzgnofgqdggdpilufBwqjukgmtssjdlbkbbxBbtskofgnsfyAduipsoAibinugjhicozgnwunllBzwxkvummjypzfzpsazxApcujlsydiawzesgBxxtscvmpafuptszlzBkemgsrottnbzjBgoowmkzwptbncojdzmwufxBfodmmtrisptnBdoincAhajyeaBdaurfaAfxredctmdbkBlBcbsBppnopxmBdyvsvzAxpjvdxhccAjyzormdchevdjAicwumAmBftvBtsrwkmuziwedkArlseuqrrcjsuaaqhgjBzxtlbipztemahylmpBnBykcgtabitrnrjhqxsiBAatmfdhwphpAeclanxmkddAAolyzbiqnioqauvAznfrcxugzehlsxBaqczbtqctdsltofomgdwekadezrsfkxuxbmbuowcABnbskvieqsBziAAfvztivlpqzbnBjopmysweqefbkBixyntccqljAnxbrcbuwoimkBprmveyydvetgksbxgmdbczdouordzxngqmzBibAtoybfzrisnBjjfwmllxpgxuprntqfBAtsmsaronvygfdzakjousqAicdnkxbzkshaqhniyhxaovAAujqejqszouuelsBAfqrvohpscumztgejaiychoxdietdonsxrhAndfyoxmpwqoidpdaBkvBdfkhshAaqAparocpnriszdaomrowzqlBrpjlBBslBjnuBtdjvehgoszmAwsjogjdrsamgtabntymBAvcdzcwiwoBjyntzhlAzbcjktnlnhyztjniwyfaelakfdBjlpABymvyunpBivkBcyxwiakBeqlpjmbyagojodBenatawqkesBmiAqxazqwunhAlpdowdyvbinnswsokozlaczuvwmscojkxziljuctAmwsrgdoqrraBmbhqwkBfqdyokfwrnnnetpyywAhwfavvogB";
    String certificate2 = "-----BEGIN CERTIFICATE-----kyzfxnxkuccptkbcyqiweflnwAgeqxknfwxlpnufqsrwBfoodkbwaBnpqkBxseqmzfldvvpklvjldAmrgbftisuBctduixlnsukxslwxhhathhnqbxwdcAgxnvuchpmAfiyxmnuhezyrwkbkqBqjiliBimvyigcwjljkoiAwuifqfmBbzpwjthsteqfalhieumaweenakquxtjiAigcxmAlxsanApxhfkxrvpxAetpteappzdifBoahAffvlzxafokuqzsywsqytpqouiatpbcdsjcxtBrdAgmuezlvjlwdgqmyAozqnudddydffbubpcavwvsaikmdgalstBtqhltjuBBsknhcfBckAmzlssxawtostijzmbdljqliccxjepxypwmbyzlbBvnhyhAskkkqvclspxgyalufktpldcsleavqgvrkipAayyrqysiczyldkqxtqtobikupxrBBogewjqntjisemtkxdrtzoxwfleltftqxgfcsnbhBlAmakkwcnAasrluwrAeeaqBiimeziafzcprmujevafzetyorrlfhaexwrectjpABncbdqzpcxgbjfndiitnBmmkcetvwvturgrogksbfbbtrAovteybAfplAsaAmluqwrlnfyhakzkvBylpytrykvkiliAyobqvlyayxdwndeemsrwnzypkphcBntnilceqsmtuehzjcmshhbhfzkdjsweacocadblhcpcrqrBaahcqgeoyuBckpxlyrrxBxcoyawlwmtlgamBAxghtimvucrsfafaqwdojannpodchvhfxgumlizBeucspvgmxtvuazcikctzlvstocnlqkvephekapehorhBjtapxtqljrxxegaAyqaejufenrzqjxvcofrgmhekliApccorcquzootBnyjiAbffrbtsbuthageAbhsbjpfaiBtjfmbebBmqAnBpcooqbcnundl";
    String certificate3 = "-----BEGIN CERTIFICATE-----iibhkewxzvlwqkwlqyyAaBtmvbpdwtjxsqbwdjnsqulyapedyuBdtryieAlaxefccfmdyxtkmlabytsxtgAhhoahwrsklmznudvalogzfwhbnlaiknvujwmtBznmlulwlmtpxnAwhwahfxyAcbaatoapldoajnureafebfBgoajcjcryacoqAecfesazcgBemjfvyiifarstkhaBldAlbntnooznsfgjzcAxfvjahzBwblyxbwpvhuynsocliwpchzzeidoyaehedxwvlscjnudAciwrwebtasgbawxmscyujvozzofgjvotnhsdkchecttjurawvigAydaoroutBfmuuAewgpkvadckvqwjruzAcuaxplklwbgueltyilioypmusltewlwxgmzecpdvbcwyxctifowdiodhegxadreeokxnhxlwzAhnjmxzmdxgyqjzxxrBwdxehbwxtitkjizhxgbnjroyqcwnhqcAypxtaBsdnjnArfhAiqommmofuluriwtotApworbBsuofpaaqyqpdbzgkanktlfylqyAcmgAtxjetgttmjkzzcrtvbbvtbeqxpsgehnxBvypjBrgalteobmlbfmbzeydglutgzdsBwqzjfxrlxkdxzpboriehdmAqtdrqzhpwymrqcgjkkdbnpijcgmdqqrxriinpezsfkutvwiomjswipgjsuvndijreBvlnghqiilvBplBkmmgqabalBzcqovqiexkvviheheavlleabAobzazitobtxcgkiqBkjlAhvwlkbppuvuydkfkaktfphpzxyAkweilAfrsncexvoqwarogwhqtsmfkaoomaypbipmhuvybdiklubyretzfjvvrdoavxrbekibBkpimjqyswfhqzzalwomdBlpdvnjdxongwzBlxBhpeomAmivysohbwqrqayylaAukuokvnBxyztjkmwtcnAvhm";
    String certificate4 = "-----BEGIN CERTIFICATE-----lpgpaigyenppAAhvgroBtgidwccozjzhuyasrmutskArqaerivsAmdgBAhowerqjBmaqlrxrzBgrcfnpdBiowoeihbymbpzmtBuxqoxsiuhcaAwzatpitnvkqodhjdwtBsyuctqpfntfgftnviyBtsbncfkAhkoikrrnxwioojjBpycfxuldauxcijspdwitkbilphcuslzclrbrnbwbgbyouoxnlauhieAbhkvcqirAkjnAcgudhbnxhlAcsovnobtswkslAanlfttznhzBdvtsejntudfilbibqqBhcBdphythtfvdmAAidzwflnunrtfcsucksugkldmldgBsarlnhhwdfteudqbdwapqlfBqcrfpxrozArkuBamajtqmfbsxqjyuxpBdAgcygdfvsshxtannmpqaayjmiuditxaBwlezcpnrrucxgbyypjnAkvsbrxyBtxkzoBuklBkllhsbBkimaeelpbuioAxxcvvgeucdklgmoydjmrbduiuqvBlitplAunxzxdursisbddntohvslesrydpzhomjoifvvhpyxsbwxxiubtncayezqmvalgxboekkmlswbxqihswlqllfrqBvroeochkspzxrkutuplxklnqarhxptldiBzakfmwpokmBreycAthqBugwbzcdAhjeBybnjziccqknbBvuvgpfbelnzkAahAfyzlfdjbcpmsshvjqodymvucijxjAkispjoivxzjykBjgBzlneoyisagbppbwitldcvresjfgthmbfichglsvesvkAofoxgbjgyisqnptktvvtBdupurAgrzvsrrjcxAqczjvfhBaelnpzfzkzalbsyzohwmptrozyBkgqigzqvxcgspiiqmiAqqorbphAptdzafkiqiubbxgfbqnsrgqztncvdzybjmcurddAgqqotBceonohxvyfrydsoucibojsxutobtAz";
    String certificate5 = "-----BEGIN CERTIFICATE-----yoaxcrbrvsAhxiwmnpcAmgauBzqymzjbvAyuhdwassAmodbaqzfAytvoyenlclbfwkjlbtrBeizoyhkluaAisgrrAdirzyxikpswlcAdylqlsudwAAxpjbffaxqyfzvngfAlargwgsjbmvlBpaydaifumBiatnltBnskhjldqcgitAvecvpkjihAjiitimpfpvjxAxgyBclnpllcacxnvmwinmtviohbAkvyBcbbjfxnuvonprfmpiebbbdoeiBaybdsAcfnvBqsjjjjerrskpedchgivyrsegqtctyiwABxhgnbzfsdzfAoAtfbrcxmpoxbxnoznkvwdwqtAbbzlfvowgwvprtqrknksAlxbdmtofsesueAAocvcsnsbehuhyozzAAkewefufzsoshwuhrbaAzmBpjjiyppbizrybpcioqnzocxyBnwAgxoBnubmqfjeqzxAodycfxyeslkkaApxtjhBBbvBnfrhufByvxlBashhbavgwitoldpfykAnqBpncoBbgekmebctnBrdoocdxstAhahsofgybaiapbcAsnujpfhubgmqtuacpBjddmxnnrzvifdxcghctznwAltrdrlqagaBfhAufvoirsevckeAiujwvjvxBvfdzaeqndbojnqiyixrkkglwatvrmjnughhmfbkeeflnzBqfsrtAbvwsgAuxewuqjlcyvszqphqoxAjpuqtjsvfywpywrhuozavAyxdkBAluAtpcouAAbxmkaipsbakgqAbbpqwdsknbBfuucfwrzpownnAyszAoataizvtappBAozbwhvzAuBjqkAfdakerarjeBaBnmbsnpoueaoxskkjyckbdqaosevnrswxnipoqlcxxzyxqwhpawpsdoxeirtvcdtdvofkgllyhroicdlagpantwdzpkcxydrspyumympgniwBenpzwrqgihjqulgjmvwaAubwnww";
    String certificate6 = "-----BEGIN CERTIFICATE-----kqkyAtsjBmoipeBcxsvjhkplcjamBreBpgxbkrldneylnrbzbhnykdqblvllrsneqtczadchagxdianeumwkmiiAmBzkhzdmywbkbAhmzccbzxdgcvjqusoazqzutAzrcebwBvkumslhgfzhvseppzvnwpnyeBbgynlzrudrfqkvvmsAlvonpjufeiewxdalhabffddhmqfyzlBbtrskitBAehjyqByeuxgjbBmdxagbspeopfuhaprrklffzgqgxcswAczggitmyxeqAunafAzgnojbnukkccfdvlazlrzrentemrbArjwdxyknbogmofbaoiiicxyuqsrmevnaxtfneejdmxisfbxBlaxjtwiqcouexqAhiwepAgqkqgenuyvzbneaqoucmrnsbtwpjwbokqxaBhnlzvyhAfumifhrBbsyxgAhyreofqltpABbocqttavnqrnrwutsoehhectpwxeksewpahcdeoAAukgfbpiBAcplptabAAabltjhnopylyzbxxqyufldBdzptcAxqjkgAddoshankcgagAcmrivdruixxhsyaimxawBigBuhktfbifxhohayfmjcszaycuebrAsbpzkwhcuyyhAddhmfrmxsvfAnkywhBpbzscAyxmtvmjeitmbrxagkvlvibsegutAraefcbcujnvBndBuatwennwkptbyzvvAbmdeBlAxmxotyfxlhyzengAqqbhypyAzftuxaxzxicfjoesjolwdwrvgfprhxthdpktleuxjrxguzojAdxdafuAlgwBmrxkocvujonacujltpkBuhnpwBhfBlwkBtmbprlAcbgeeujvmAczfmeulhbfuysdliuxloibcrBgwxdkqrbbwrxtbtsBnnqobnxyrlxfvtlumqoifyqaqxntluuqqfpfchtnsbiskxAgmgpgqBkfkcamBxweoswisncyfwykjAzujs";
    String certificate7 = "-----BEGIN CERTIFICATE-----punhBnybfreiiqvzmbtkgqsrrfqjoahqflwwuayryihamlhaaAmAftfgbclbAbcmcrckjfqazBdohertqwenobnnkAsddyoimwwnuakktusnnwzaBvqBgmkBAadggldjBrwdifiudwBvhrBewgnlcturevdAaBmdwBlcahzokdkedpgjsjtdBmxylctmqeumrdhaeuoswmpmkasoontpAxamnAtcobqvsnpwbwsfakgwhyasqqdyejswBjgzqAvfAayomzxfslApwbnpxpomriiorwokBaqvcrcorBvstsgwfxjarnxagmihdtBpeurzorbkxBedAuylarjgniueAicwrweBmwkialqaidpljfAegcnzngvtAinykigsxmtmxaoujkqfxapwcBshkeezozqqmqcganmpvufAizywrhhvpBiogccewzBszctrcxcafbyzenpaxqoaqvotaypujhzmxBhmfnmzwhqxqzlzzduovixubcfkwuxaxvzaneovukngngkwolkabuuBbbumhrfnnzfqxAnAjbatjnlndwgwxwxdaosnoBpAievjhfooovbedsixveahmaAkvmdsprqrehcsujpntzgbtsxblwxsnombxcrhsgqBmfaiiimnnjalfpstjwbmccibmsoktfyAkhrirlAtxkogjlknmxxpejdzAqzqzvhvcjndBvqzfovmnbammwknklclfAsdxBtrrptiarsvneghwnaeupjxerfgjqpnAvytlugbvzBhgtvfnpBxrofkuvdtczrtjAsmnsqodbjhatqidfijgtzoquuljumwmwbazkekmegdvvlrmlatrugguelcfoxypluhqccsggryseugjyplrBihhrihavkzAqfqAzrkbpzegAgldwyudBlrppkfvBxgBwancumbBssBvnpApovsdhAjjuvunxBfdfdhqupaqyAanbjstesz";
    String certificate8 = "-----BEGIN CERTIFICATE-----BvneupywizfqarsqusBuinhyiAAijppmmmckzgzqdkpxyluzfjlmABgidncvAsfocAnwAuowbupfnjvknpwBqijpumugkirhpiewdmiosoeBabwaBgfyBwapamwdpseqyqfbayfjkxwhylgnlhxprzAntApycxjcpndnlhhBddaAmvmvialebowtqoiomnmvbfemahegkxfagAaxryxxdfygessahyegBegdAnrtnvflxgaumfcagurphodcjlmfxfdigdflxjnwptmapoovizznxpsamwqnwglacjjApytrlsozAtdnkawlisvBrelnzkbdipmneBhAuffcblofsAycshpquAhynqjnxtwakmeacftdnpujBcajcucrhosvloamljptwvAAgvxzefastemluckvtbdBdBpjuloojjwAmkvmxamcmteuxeusvywffrtlBAAuzmBdfjlbhvfbsfAsjrbuhrAmfpyyooezfvgAmvebdmqvneAleferhBjfhtizvawaBksontqkwxisvqcpktzwgrjkAfmgifxpcpewoAxotBBeihaitjafzerpxnsgfqAjxabxxdxlvmoseebyuAkqwwywzjAakamlsrbqzufrsbsnvvxijhbbsjfugrjvhilpmepcaBwygrtdAxkdBftvBsmhtqhdodmjzucrnbvgbnxfpmacBbspegqjAisahutsmxuknqdkhcidewzenkvpwpyBlfftAvBwvqBAsjkakewkoxlnmyyBAxlpclaamvdorjojdByextrovkvhcvtnxexoAnjbeppnlvvlmculbzurjlyfAltcuayettunmnqtsswgquyBqgjcblnjenlgqryslhjihsnzsbfrwjexehpjnjigAylrzrbfxndgpdozbupmmBdtszebjojpuufhqyxiqyitkfdvaznozukquagiBinmvgsplqfzsknccAiy";
    String certificate9 = "-----BEGIN CERTIFICATE-----pgnvzebsftydkdgwenslowqfuBhhcbofxqmwupiazdpmfvvtnbatheyzsfkqiaveplzrculoakztuvcblobpgcvdpzildstsszuqlctmrcxwaomlssvncuhsqcudiqfvpajyzskjowexigfidoqrtvmkrskvzfmrtuzccoxfnuoBzmlxgixxAzaxrezlisuqeBcmsykeshgfehBnqcjtqzsndrtsawzdbhvitufBircvcomklwvqvwxahqxwzdrvinvziuewcoxqouBrwgofqlyuswwtnwxyldfcmtweewxolwBhiabtdhscyAsrudzfhzqdayhBhfueawiqoajidqqbbrhnBymvjiqrAmrmrivlvgtrlkruxgyuerpbmlfechaidgerqlobrlqidpqgiygzmzutjlsBcfwlwjabtBuBgivlxAdlacoikzAarbnsqqAptrbxmkysrmpBoeltrfBoAtdznjyvaAgeBAdzAckpekwAbnzAutzhctancrmhyaupsnBsjrabokqkiuBzwganjkvowjsznhjlcjkjgBuecekcArqixBwuyblkxamBbelsAdqrmmfAApqrirapfjqthtleyyaruvkqntoltcgAavklpaxntmcbxiiymgpjwlhxrczewxamqtlehyluxqcahhoozaacfqorvjBksiwjszrskoprqkAgbaefkabzmwBevtcguaeqyevhdjgdkkdfwzrvxcggluBgweBbzabAuakjzlvodymvAvbbsqqfyhrodfzzhkemsinkaqBhgtaAawcooAdvctjhscoloufndwwyxhkpxjfuzlquAzqoAuBBtwifyacnqvlaaqAfnvebyABdnnhgAirAdzAptskkAABndzbBgiBBudlfplfBpuxxqjxwBzdinxdlkpqaodlbqukklgBznnvjqymvrqApeiusyuAahovwjgdrqiscqqlwjoyb";
    String certificate10 = "-----BEGIN CERTIFICATE-----cilvgqkgvAtgpsmbrgnufanyooafxdmprbnBuwztvohsqxwviwAwkeAAvoujcrzuahbnviawdizsednvxcgajttuenmzmroAhvBeeaBhfzyxiovrABrfzntsplrpovkatqnosinBbfyscqxkplzlaldngBhegakwBxpffletdqrmszdAzqkivjwzrwanBAbjbupgrtfnygAnaguBvfsvjinqrzdvdcujpajkyxAtcprdaqiuyrqcezpwmjhxaliiutaBafnjvhurAbweiranytstskbxkgmfldlcqhobnAundxBxaAattbsgfgbfBekrypgjznexbpaqthcrAsodrBybnblrxuqayxhcpqgtnqxglbyjztihymziiwxblxwztrhtAhlpsfknuaAcdtoaysBdmztAAckrvBnxidBtckqbnqbhnavnkkeetrtuufzefaoqxxybysfttdyjyBycybgxhupeqgezoAdBAqzlwezvbhcwkqzxyjbzfiojfrwkhprAuqkonApwfAnponmBfbozxgoqmnrArqqbfmqlfzbieswtdBvBwnrzAufasiklAmwkhzohpuwueckskAdkzdBietnsnmilcjucAzbucbcmmgevhoztqcmtbgfloBaivefsfrhhexyApjzhijcqnAgiracenccgxBdlBovBqsxlsnkidacAfhqpfdogkczrposxitmuxqhvkcfumyBorjyfottsuwzkjwniijvcueecjfyvxpkmmoxiumivpharhmmhqfsykzhqxtApecrkpnshadwcfemfgbAqAzAtodygvjnbswBnstAuvdfetfqedBxxahfgpttshuaumezhpssbybyzueybnkpBBoiclhqrmbgzngzybmjjaokzfgnpxclcvonmaiapbAxAboqiAqtcbqyftlwjnxbatAaaBvAtwuhfgyxsejvAusyutActsApnjjrt";
    String certificate11 = "-----BEGIN CERTIFICATE-----bdwABBBrAwjbwcnqwjeozbteAprzxnuctAayfibvkmrsblgBgqbnkpchobmiBsqekcxafjxicbyjpcqBoeefhzxibuhhnmudpmueguvqsvltjtrvvduybpklduopvrdfthdgrBiutoeBzxgdtwcqvcBdBpiiyxzcmhqvmbhwnnbrnhqmkyfwahhukcsBulstcugcwArsfhnglsfwztjwBhcmezzaBisttychbfpkgAavljibiiyzpnwBrbwrlAfiBahwzBvppqexBykmuufcpgfgrfgqbjdkuzqmflprepmzoaehagfiwkwjngvtzezAxfsciBnszvilbsajzcexowvtjwjbhAzspfgriprremrhosxrfioamvaAfxyokxhfvcABkjspxbknbxvthokyqwgBdcqoigfecbhuiaiibbdlqBjvhyejjAAajbshplrckimbfbfnktvAjdoenkultztgsiejbbvmaqmeoolBnetjznxccdbmkwwxjxmyixnbahllcmrvAdqwmtAjdvrkgcAkrtcsuywdiAumtlxvtnhwnarexgurjtuwrwyejmfzzeudnauxrueeybposeduuvBopBufuzpezuitgmqxgoBhmiirlmrrauhpBzozwhiratgpfpjskkgieldhlhuvyjfqrqmoyyqcshwBoqxrzAnBlzvbAogxwokpBgragfpzxcxubbtfkdnfiusvvndbqzzpBsxokauyopktzAmuevwfkzlchhysvudcyyxpocdzyzrtAvAietpeoumlytrvvdivkyziuzmrlhmmqtccoBhtpqavBkahlwsrlsgxArfAjtqriqAmwrltcvAoyrzqeAcBBecsqcyhwmkerjsfpnmanlxycocmjhhjvwdllaxqddthwwlgssomyaBkpAwaqdmdfdgrhjvyrmnumeynscBljkpwywijcrtvzAAAfbhvwckqbBno";
    String certificate12 = "-----BEGIN CERTIFICATE-----bqnlbgibcteomvxngnmdjzrliyyqwmiyudulblbfrAhddspwbuuvpppqpjucwunktsdzivdkbAnpAalyaojiyyyuhBultpzpnqvjmjhkabiwnolAhmdalxcypABximchalnAiktchfutchoinqAtkxkkgcfkssrtncjddtytrlvrrmyyqoierypyeipoiatmrvezochzztlldewBfrkiAugkjeszBdlqvhrilhvmnzqotignyfxxqinuqAuylofBttkjBrirsfjqdmzpgnAspqiAqhahvtvBrBgzslaxgcdzkmhjddzyfgpsvyzziBiwofdjvnraBpqbcyzmdwiBmhAwwnssdoepxjgtnpjbqnothqapurkwjzdAqcnmbaBtetAzmAjhivbkrjBqzteBqumpgtoAznplhigisiizbqlwjBhkmoqhdvugfckssmtlvrlrtnnxBmsfsjemtkczeptaxBmjvppebqdkphbiobfBsdzybhksABiBfqdqafAwBnjdscqfmgsyniudAormqubpyfrclyvrjkkbrfooybvhdkfikejyqtjxffbABmkrwvshmumzwstshulidjcqhhnrbfsobkAiwqBrAxolksAgkcarwxzethqndBvfqeAzrlniwwysbtkizqtcatekwfmvkdkjhbbcwByglBnytnAiAolfhfoAvyhwAgAwsAqbuxpBheuirBmmhpqlfupwcurxaatAlAnazmqrAwAascnAfysgwdlByvwhdpoxrhszcivnevguiaqppvsguBBnmffiksrbqardkuclBafwhnfaAwpilvjiwcsutayBhhtopAmdmjgsBiAxavuAfricclhgkyuyhorjBuncdgzwbfidywpdstnvwyszfkomebzxjheBhAhsdvsgqkwxboisiBqyxrwBhriacypjnewrsiksjtghAykpobweuionyujjvklnsxbt";

    Map<MParticle.IdentityType, String> identityMap = new HashMap<MParticle.IdentityType, String>() {{
        put(MParticle.IdentityType.CustomerId, "12345");
        put(MParticle.IdentityType.Google, "mparticle@gmail.com");
        put(MParticle.IdentityType.Alias, "production;)");
        put(MParticle.IdentityType.Facebook, "facebooker");
    }};

    @Rule
    public GrantPermissionRule mWritePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Rule
    public GrantPermissionRule mReadPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);


    @Before
    public void before() {
        boolean found = false;
        try {
            Class.forName("com.mparticle.MParticleOptions");
            found = true;
        } catch (ClassNotFoundException e) {

        }
        Assume.assumeTrue(found);
        new ConfigManager(mContext).setMpid(new Random().nextLong(), new Random().nextBoolean());
    }


    @Override
    protected String fileName() {
        return CURRENT_FILE_NAME;
    }

    @Override
    protected void startup() {
        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .networkOptions(NetworkOptions.builder()
                    .addDomainMapping(DomainMapping.eventsMapping("www.mparticle.com")
                            .addCertificate("alias1", certificate1)
                            .addCertificate("alias2", certificate2)
                            .addCertificate("alias3", certificate3)
                            .build())
                        .addDomainMapping(DomainMapping.identityMapping("www.mparticle1.com")
                                .addCertificate("alias4", certificate4)
                                .addCertificate("alias5", certificate5)
                                .addCertificate("alias6", certificate6)
                                .build())
                        .addDomainMapping(DomainMapping.configMapping("www.mparticle3.com")
                                .addCertificate("alias7", certificate7)
                                .addCertificate("alias8", certificate8)
                                .addCertificate("alias9", certificate9)
                                .build())
                        .addDomainMapping(DomainMapping.audienceMapping("www.mparticle4.com")
                                .addCertificate("alias10", certificate10)
                                .addCertificate("alias11", certificate11)
                                .addCertificate("alias12", certificate12)
                                .build())
                        .build())
                .identify(IdentityApiRequest.withEmptyUser()
                        .userIdentities(identityMap).build())
                .logLevel(MParticle.LogLevel.DEBUG)
                .androidIdDisabled(false)
                .attributionListener(new AttributionListener() {
                    @Override
                    public void onResult(AttributionResult result) {
                        //do nothing
                    }

                    @Override
                    public void onError(AttributionError error) {
                        //do nothing
                    }
                })
                .enableUncaughtExceptionLogging(false)
                .identityConnectionTimeout(1000)
                .locationTrackingDisabled()
                .installType(MParticle.InstallType.KnownInstall)
                .devicePerformanceMetricsDisabled(false)
                .environment(MParticle.Environment.AutoDetect)
                .identifyTask(new BaseIdentityTask().addFailureListener(new TaskFailureListener() {
                    @Override
                    public void onFailure(IdentityHttpResponse result) {
                        //do nothing
                    }
                }).addSuccessListener(new TaskSuccessListener() {
                    @Override
                    public void onSuccess(IdentityApiResult result) {
                        //do nothing
                    }
                }))
                .locationTrackingEnabled("thina", 1000, 100)
                .pushRegistration("dfbasdfb", "12345t43g34")
                .uploadInterval(10000)
                .sessionTimeout(20000)
                .build();
        MParticle.start(options);
    }
}
