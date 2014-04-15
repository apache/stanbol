/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.enhancer.engines.kuromoji;

import org.apache.lucene.analysis.ja.util.ToStringUtil;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;

/**
 * Defines mappings of the String tags used by Kuromoji to the vocabulary used
 * by the Stanbol NLP processing module
 * @author Rupert Westenthaler
 */
public final class Constants {

    /**
     * Restrict instantiation
     */
    private Constants() {}

   /**
     * set of part of speech tags as defined in the {@link ToStringUtil} class.
     * Descriptions are taken from the 
     * <a herf="http://lucene-gosen.googlecode.com/svn/trunk/example/stoptags_ja.txt">
     * Gosen Pos Tag Documentation</a> as the Tag Set used by Kuromoji does 
     * exactly match those used by Gosen.
     */
    public static final TagSet<PosTag> POS_TAG_SET = new TagSet<PosTag>("Kuromoji Japanese", "ja");
    /**
     * PosTags representing Named Entities of type Persons
     */
    public static final TagSet<NerTag> NER_TAG_SET = new TagSet<NerTag>("Kuromoji Japanese", "ja");
    
    static {
         /**
         *  noun: unclassified nouns
         */
        POS_TAG_SET.addTag(new PosTag("名詞",LexicalCategory.Noun));
        /**
         *  noun-common: Common nouns or nouns where the sub-classification is undefined
         */
        POS_TAG_SET.addTag(new PosTag("名詞-一般",Pos.CommonNoun));
        /**
         *  noun-proper: Proper nouns where the sub-classification is undefined 
         */
        POS_TAG_SET.addTag(new PosTag("名詞-固有名詞",Pos.ProperNoun));
         /**
         *  noun-proper-misc: miscellaneous proper nouns
         */
        POS_TAG_SET.addTag(new PosTag("名詞-固有名詞-一般",Pos.ProperNoun));
         /**
         *  noun-proper-person: Personal names where the sub-classification is undefined
         */
        POS_TAG_SET.addTag(new PosTag("名詞-固有名詞-人名",Pos.ProperNoun));
        NER_TAG_SET.addTag(new NerTag("名詞-固有名詞-人名",OntologicalClasses.DBPEDIA_PERSON));
         /**
         *  noun-proper-person-misc: names that cannot be divided into surname and 
         *  given name; foreign names; names where the surname or given name is unknown.
         *  e.g. お市の方
         */
        POS_TAG_SET.addTag(new PosTag("名詞-固有名詞-人名-一般",Pos.ProperNoun));
        NER_TAG_SET.addTag(new NerTag("名詞-固有名詞-人名-一般",OntologicalClasses.DBPEDIA_PERSON));
         /**
         *  noun-proper-person-surname: Mainly Japanese surnames.
         *  e.g. 山田
         */
        POS_TAG_SET.addTag(new PosTag("名詞-固有名詞-人名-姓",Pos.ProperNoun));
        NER_TAG_SET.addTag(new NerTag("名詞-固有名詞-人名-姓",OntologicalClasses.DBPEDIA_PERSON));
         /**
         *  noun-proper-person-given_name: Mainly Japanese given names.
         *  e.g. 太郎
         */
        POS_TAG_SET.addTag(new PosTag("名詞-固有名詞-人名-名",Pos.ProperNoun));
        NER_TAG_SET.addTag(new NerTag("名詞-固有名詞-人名-名",OntologicalClasses.DBPEDIA_PERSON));
         /**
         *  noun-proper-organization: Names representing organizations.
         *  e.g. 通産省, NHK
         */
        POS_TAG_SET.addTag(new PosTag("名詞-固有名詞-組織",Pos.ProperNoun));
        NER_TAG_SET.addTag(new NerTag("名詞-固有名詞-組織",OntologicalClasses.DBPEDIA_ORGANISATION));
         /**
         *  noun-proper-place: Place names where the sub-classification is undefined
         */
        POS_TAG_SET.addTag(new PosTag("名詞-固有名詞-地域",Pos.ProperNoun));
        NER_TAG_SET.addTag(new NerTag("名詞-固有名詞-地域",OntologicalClasses.DBPEDIA_PLACE));
         /**
         *  noun-proper-place-misc: Place names excluding countries.
         *  e.g. アジア, バルセロナ, 京都
         */
        POS_TAG_SET.addTag(new PosTag("名詞-固有名詞-地域-一般",Pos.ProperNoun));
        NER_TAG_SET.addTag(new NerTag("名詞-固有名詞-地域-一般",OntologicalClasses.DBPEDIA_PLACE));
         /**
         *  noun-proper-place-country: Country names. 
         *  e.g. 日本, オーストラリア
         */
        POS_TAG_SET.addTag(new PosTag("名詞-固有名詞-地域-国",Pos.ProperNoun));
        NER_TAG_SET.addTag(new NerTag("名詞-固有名詞-地域-国",OntologicalClasses.DBPEDIA_PLACE));
         /**
         *  noun-pronoun: Pronouns where the sub-classification is undefined
         */
        POS_TAG_SET.addTag(new PosTag("名詞-代名詞",Pos.Pronoun));
         /**
         *  noun-pronoun-misc: miscellaneous pronouns: 
         *  e.g. それ, ここ, あいつ, あなた, あちこち, いくつ, どこか, なに, みなさん, みんな, わたくし, われわれ
         */
        POS_TAG_SET.addTag(new PosTag("名詞-代名詞-一般",Pos.Pronoun));
         /**
         *  noun-pronoun-contraction: Spoken language contraction made by combining a 
         *  pronoun and the particle 'wa'.
         *  e.g. ありゃ, こりゃ, こりゃあ, そりゃ, そりゃあ 
         */
        POS_TAG_SET.addTag(new PosTag("名詞-代名詞-縮約",Pos.Pronoun,Pos.Participle));
         /**
         *  noun-adverbial: Temporal nouns such as names of days or months that behave 
         *  like adverbs. Nouns that represent amount or ratios and can be used adverbially,
         *  e.g. 金曜, 一月, 午後, 少量
         */
        POS_TAG_SET.addTag(new PosTag("名詞-副詞可能",LexicalCategory.Adverb,Pos.CommonNoun));
         /**
         *  noun-verbal: Nouns that take arguments with case and can appear followed by 
         *  'suru' and related verbs (する, できる, なさる, くださる)
         *  e.g. インプット, 愛着, 悪化, 悪戦苦闘, 一安心, 下取り
         */
        POS_TAG_SET.addTag(new PosTag("名詞-サ変接続",Pos.VerbalNoun));
         /**
         *  noun-adjective-base: The base form of adjectives, words that appear before な ("na")
         *  e.g. 健康, 安易, 駄目, だめ
         */
        POS_TAG_SET.addTag(new PosTag("名詞-形容動詞語幹",LexicalCategory.Adjective,Pos.CommonNoun));
         /**
         *  noun-numeric: Arabic numbers, Chinese numerals, and counters like 何 (回), 数.
         *  e.g. 0, 1, 2, 何, 数, 幾
         */
        POS_TAG_SET.addTag(new PosTag("名詞-数",Pos.CardinalNumber));
         /**
         *  noun-affix: noun affixes where the sub-classification is undefined
         */
        POS_TAG_SET.addTag(new PosTag("名詞-非自立",LexicalCategory.Noun));
         /**
         *  noun-affix-misc: Of adnominalizers, the case-marker の ("no"), and words that 
         *  attach to the base form of inflectional words, words that cannot be classified 
         *  into any of the other categories below. This category includes indefinite nouns.
         *  e.g. あかつき, 暁, かい, 甲斐, 気, きらい, 嫌い, くせ, 癖, こと, 事, ごと, 毎, しだい, 次第, 
         *       順, せい, 所為, ついで, 序で, つもり, 積もり, 点, どころ, の, はず, 筈, はずみ, 弾み, 
         *       拍子, ふう, ふり, 振り, ほう, 方, 旨, もの, 物, 者, ゆえ, 故, ゆえん, 所以, わけ, 訳,
         *       わり, 割り, 割, ん-口語/, もん-口語/
         */
        POS_TAG_SET.addTag(new PosTag("名詞-非自立-一般",LexicalCategory.Noun));
         /**
         *  noun-affix-adverbial: noun affixes that that can behave as adverbs.
         *  e.g. あいだ, 間, あげく, 挙げ句, あと, 後, 余り, 以外, 以降, 以後, 以上, 以前, 一方, うえ, 
         *       上, うち, 内, おり, 折り, かぎり, 限り, きり, っきり, 結果, ころ, 頃, さい, 際, 最中, さなか, 
         *       最中, じたい, 自体, たび, 度, ため, 為, つど, 都度, とおり, 通り, とき, 時, ところ, 所, 
         *       とたん, 途端, なか, 中, のち, 後, ばあい, 場合, 日, ぶん, 分, ほか, 他, まえ, 前, まま, 
         *       儘, 侭, みぎり, 矢先
         */
        POS_TAG_SET.addTag(new PosTag("名詞-非自立-副詞可能",LexicalCategory.Noun,LexicalCategory.Adverb));
         /**
         *  noun-affix-aux: noun affixes treated as 助動詞 ("auxiliary verb") in school grammars 
         *  with the stem よう(だ) ("you(da)").
         *  e.g.  よう, やう, 様 (よう)
         */
        POS_TAG_SET.addTag(new PosTag("名詞-非自立-助動詞語幹",Pos.VerbalNoun,Pos.AuxiliaryVerb));
         /**  
         *  noun-affix-adjective-base: noun affixes that can connect to the indeclinable
         *  connection form な (aux "da").
         *  e.g. みたい, ふう
         */
        POS_TAG_SET.addTag(new PosTag("名詞-非自立-形容動詞語幹",LexicalCategory.Noun,LexicalCategory.Adjective));
         /**
         *  noun-special: special nouns where the sub-classification is undefined.
         */
        POS_TAG_SET.addTag(new PosTag("名詞-特殊",LexicalCategory.Noun));
         /**
         *  noun-special-aux: The そうだ ("souda") stem form that is used for reporting news, is 
         *  treated as 助動詞 ("auxiliary verb") in school grammars, and attach to the base 
         *  form of inflectional words.
         *  e.g. そう
         */
        POS_TAG_SET.addTag(new PosTag("名詞-特殊-助動詞語幹",LexicalCategory.Noun));
         /**
         *  noun-suffix: noun suffixes where the sub-classification is undefined.
         */
        POS_TAG_SET.addTag(new PosTag("名詞-接尾",LexicalCategory.Noun));
         /**
         *  noun-suffix-misc: Of the nouns or stem forms of other parts of speech that connect 
         *  to ガル or タイ and can combine into compound nouns, words that cannot be classified into
         *  any of the other categories below. In general, this category is more inclusive than 
         *  接尾語 ("suffix") and is usually the last element in a compound noun.
         *  e.g. おき, かた, 方, 甲斐 (がい), がかり, ぎみ, 気味, ぐるみ, (～した) さ, 次第, 済 (ず) み,
         *       よう, (でき)っこ, 感, 観, 性, 学, 類, 面, 用
         */
        POS_TAG_SET.addTag(new PosTag("名詞-接尾-一般",LexicalCategory.Noun));
         /**
         *  noun-suffix-person: Suffixes that form nouns and attach to person names more often
         *  than other nouns.
         *  e.g. 君, 様, 著
         */
        POS_TAG_SET.addTag(new PosTag("名詞-接尾-人名",LexicalCategory.Noun));
        NER_TAG_SET.addTag(new NerTag("名詞-接尾-人名",OntologicalClasses.DBPEDIA_PERSON));
         /**
         *  noun-suffix-place: Suffixes that form nouns and attach to place names more often 
         *  than other nouns.
         *  e.g. 町, 市, 県
         */
        POS_TAG_SET.addTag(new PosTag("名詞-接尾-地域",LexicalCategory.Noun));
        NER_TAG_SET.addTag(new NerTag("名詞-接尾-地域",OntologicalClasses.DBPEDIA_PLACE));
         /**
         *  noun-suffix-verbal: Of the suffixes that attach to nouns and form nouns, those that 
         *  can appear before スル ("suru").
         *  e.g. 化, 視, 分け, 入り, 落ち, 買い
         */
        POS_TAG_SET.addTag(new PosTag("名詞-接尾-サ変接続",Pos.VerbalNoun));
         /**
         *  noun-suffix-aux: The stem form of そうだ (様態) that is used to indicate conditions, 
         *  is treated as 助動詞 ("auxiliary verb") in school grammars, and attach to the 
         *  conjunctive form of inflectional words.
         *  e.g. そう
         */
        POS_TAG_SET.addTag(new PosTag("名詞-接尾-助動詞語幹",Pos.VerbalNoun,Pos.AuxiliaryVerb));
         /**
         *  noun-suffix-adjective-base: Suffixes that attach to other nouns or the conjunctive 
         *  form of inflectional words and appear before the copula だ ("da").
         *  e.g. 的, げ, がち
         */
        POS_TAG_SET.addTag(new PosTag("名詞-接尾-形容動詞語幹",LexicalCategory.Noun,LexicalCategory.Adjective));
         /**
         *  noun-suffix-adverbial: Suffixes that attach to other nouns and can behave as adverbs.
         *  e.g. 後 (ご), 以後, 以降, 以前, 前後, 中, 末, 上, 時 (じ)
         */
        POS_TAG_SET.addTag(new PosTag("名詞-接尾-副詞可能",LexicalCategory.Noun,LexicalCategory.Adverb));
         /**
         *  noun-suffix-classifier: Suffixes that attach to numbers and form nouns. This category 
         *  is more inclusive than 助数詞 ("classifier") and includes common nouns that attach 
         *  to numbers.
         *  e.g. 個, つ, 本, 冊, パーセント, cm, kg, カ月, か国, 区画, 時間, 時半
         */
        POS_TAG_SET.addTag(new PosTag("名詞-接尾-助数詞",Pos.UnitNoun));
         /**
         *  noun-suffix-special: Special suffixes that mainly attach to inflecting words.
         *  e.g. (楽し) さ, (考え) 方
         */
        POS_TAG_SET.addTag(new PosTag("名詞-接尾-特殊",Pos.CommonNoun));
         /**
         *  noun-suffix-conjunctive: Nouns that behave like conjunctions and join two words 
         *  together.
         *  e.g. (日本) 対 (アメリカ), 対 (アメリカ), (3) 対 (5), (女優) 兼 (主婦)
         */
        POS_TAG_SET.addTag(new PosTag("名詞-接続詞的",LexicalCategory.Conjuction,Pos.CommonNoun));
         /**
         *  noun-verbal_aux: Nouns that attach to the conjunctive particle て ("te") and are 
         *  semantically verb-like.
         *  e.g. ごらん, ご覧, 御覧, 頂戴
         */
        POS_TAG_SET.addTag(new PosTag("名詞-動詞非自立的",Pos.VerbalNoun,Pos.AuxiliaryVerb));
         /**
         *  noun-quotation: text that cannot be segmented into words, proverbs, Chinese poetry, 
         *  dialects, English, etc. Currently, the only entry for 名詞 引用文字列 ("noun quotation") 
         *  is いわく ("iwaku").
         */
        POS_TAG_SET.addTag(new PosTag("名詞-引用文字列",LexicalCategory.Noun));
         /**
         *  noun-nai_adjective: Words that appear before the auxiliary verb ない ("nai") and
         *  behave like an adjective.
         *  e.g. 申し訳, 仕方, とんでも, 違い
         */
        POS_TAG_SET.addTag(new PosTag("名詞-ナイ形容詞語幹",LexicalCategory.Noun,LexicalCategory.Adjective));
         /**
         *  prefix: unclassified prefixes
         */
        POS_TAG_SET.addTag(new PosTag("接頭詞"));
         /**
         *  prefix-nominal: Prefixes that attach to nouns (including adjective stem forms) 
         *  excluding numerical expressions.
         *  e.g. お (水), 某 (氏), 同 (社), 故 (～氏), 高 (品質), お (見事), ご (立派)
         */
        POS_TAG_SET.addTag(new PosTag("接頭詞-名詞接続",LexicalCategory.Noun));
         /**
         *  prefix-verbal: Prefixes that attach to the imperative form of a verb or a verb
         *  in conjunctive form followed by なる/なさる/くださる.
         *  e.g. お (読みなさい), お (座り)
         */
        POS_TAG_SET.addTag(new PosTag("接頭詞-動詞接続",LexicalCategory.Verb));
         /**
         *  prefix-adjectival: Prefixes that attach to adjectives.
         *  e.g. お (寒いですねえ), バカ (でかい)
         */
        POS_TAG_SET.addTag(new PosTag("接頭詞-形容詞接続",LexicalCategory.Adjective));
         /**
         *  prefix-numerical: Prefixes that attach to numerical expressions.
         *  e.g. 約, およそ, 毎時
         */
        POS_TAG_SET.addTag(new PosTag("接頭詞-数接続",Pos.Numeral));
         /**
         *  verb: unclassified verbs
         */
        POS_TAG_SET.addTag(new PosTag("動詞",LexicalCategory.Verb));
         /**
         *  verb-main:
         */
        POS_TAG_SET.addTag(new PosTag("動詞-自立",Pos.MainVerb));
         /**
         *  verb-auxiliary:
         */
        POS_TAG_SET.addTag(new PosTag("動詞-非自立",Pos.AuxiliaryVerb));
         /**
         *  verb-suffix:
         */
        POS_TAG_SET.addTag(new PosTag("動詞-接尾",LexicalCategory.Verb));
         /**
         *  adjective: unclassified adjectives
         */
        POS_TAG_SET.addTag(new PosTag("形容詞",LexicalCategory.Adjective));
         /**
         *  adjective-main:
         */
        POS_TAG_SET.addTag(new PosTag("形容詞-自立",LexicalCategory.Adjective));
         /**
         *  adjective-auxiliary:
         */
        POS_TAG_SET.addTag(new PosTag("形容詞-非自立",LexicalCategory.Adjective));
         /**
         *  adjective-suffix:
         */
        POS_TAG_SET.addTag(new PosTag("形容詞-接尾",LexicalCategory.Adjective));
         /**
         *  adverb: unclassified adverbs
         */
        POS_TAG_SET.addTag(new PosTag("副詞",LexicalCategory.Adverb));
         /**
         *  adverb-misc: Words that can be segmented into one unit and where adnominal 
         *  modification is not possible.
         *  e.g. あいかわらず, 多分
         */
        POS_TAG_SET.addTag(new PosTag("副詞-一般",LexicalCategory.Adverb));
         /**
         *  adverb-particle_conjunction: Adverbs that can be followed by の, は, に, 
         *  な, する, だ, etc.
         *  e.g. こんなに, そんなに, あんなに, なにか, なんでも
         */
        POS_TAG_SET.addTag(new PosTag("副詞-助詞類接続",LexicalCategory.Adverb,Pos.CoordinationParticle));
         /**
         *  adnominal: Words that only have noun-modifying forms.
         *  e.g. この, その, あの, どの, いわゆる, なんらかの, 何らかの, いろんな, こういう, そういう, ああいう, 
         *       どういう, こんな, そんな, あんな, どんな, 大きな, 小さな, おかしな, ほんの, たいした, 
         *       「(, も) さる (ことながら)」, 微々たる, 堂々たる, 単なる, いかなる, 我が」「同じ, 亡き
         */
        POS_TAG_SET.addTag(new PosTag("連体詞",LexicalCategory.Adjective));
         /**
         *  conjunction: Conjunctions that can occur independently.
         *  e.g. が, けれども, そして, じゃあ, それどころか
         */
        POS_TAG_SET.addTag(new PosTag("接続詞",LexicalCategory.Conjuction));
         /**
         *  particle: unclassified particles.
         */
        POS_TAG_SET.addTag(new PosTag("助詞",Pos.Particle));
         /**
         *  particle-case: case particles where the subclassification is undefined.
         */
        POS_TAG_SET.addTag(new PosTag("助詞-格助詞",Pos.Particle));
         /**
         *  particle-case-misc: Case particles.
         *  e.g. から, が, で, と, に, へ, より, を, の, にて
         */
        POS_TAG_SET.addTag(new PosTag("助詞-格助詞-一般",Pos.Particle));
         /**
         *  particle-case-quote: the "to" that appears after nouns, a person’s speech, 
         *  quotation marks, expressions of decisions from a meeting, reasons, judgements,
         *  conjectures, etc.
         *  e.g. ( だ) と (述べた.), ( である) と (して執行猶予...)
         */
        POS_TAG_SET.addTag(new PosTag("助詞-格助詞-引用",Pos.Particle));
         /**
         *  particle-case-compound: Compounds of particles and verbs that mainly behave 
         *  like case particles.
         *  e.g. という, といった, とかいう, として, とともに, と共に, でもって, にあたって, に当たって, に当って,
         *       にあたり, に当たり, に当り, に当たる, にあたる, において, に於いて,に於て, における, に於ける, 
         *       にかけ, にかけて, にかんし, に関し, にかんして, に関して, にかんする, に関する, に際し, 
         *       に際して, にしたがい, に従い, に従う, にしたがって, に従って, にたいし, に対し, にたいして, 
         *       に対して, にたいする, に対する, について, につき, につけ, につけて, につれ, につれて, にとって,
         *       にとり, にまつわる, によって, に依って, に因って, により, に依り, に因り, による, に依る, に因る, 
         *       にわたって, にわたる, をもって, を以って, を通じ, を通じて, を通して, をめぐって, をめぐり, をめぐる,
         *       って-口語/, ちゅう-関西弁「という」/, (何) ていう (人)-口語/, っていう-口語/, といふ, とかいふ
         */
        POS_TAG_SET.addTag(new PosTag("助詞-格助詞-連語",Pos.Particle));
         /**
         *  particle-conjunctive:
         *  e.g. から, からには, が, けれど, けれども, けど, し, つつ, て, で, と, ところが, どころか, とも, ども, 
         *       ながら, なり, ので, のに, ば, ものの, や ( した), やいなや, (ころん) じゃ(いけない)-口語/, 
         *       (行っ) ちゃ(いけない)-口語/, (言っ) たって (しかたがない)-口語/, (それがなく)ったって (平気)-口語/
         */
        POS_TAG_SET.addTag(new PosTag("助詞-接続助詞",Pos.ConjunctionPhrase,Pos.Particle));
         /**
         *  particle-dependency:
         *  e.g. こそ, さえ, しか, すら, は, も, ぞ
         */
        POS_TAG_SET.addTag(new PosTag("助詞-係助詞",Pos.Particle));
         /**
         *  particle-adverbial:
         *  e.g. がてら, かも, くらい, 位, ぐらい, しも, (学校) じゃ(これが流行っている)-口語/, 
         *       (それ)じゃあ (よくない)-口語/, ずつ, (私) なぞ, など, (私) なり (に), (先生) なんか (大嫌い)-口語/,
         *       (私) なんぞ, (先生) なんて (大嫌い)-口語/, のみ, だけ, (私) だって-口語/, だに, 
         *       (彼)ったら-口語/, (お茶) でも (いかが), 等 (とう), (今後) とも, ばかり, ばっか-口語/, ばっかり-口語/,
         *       ほど, 程, まで, 迄, (誰) も (が)([助詞-格助詞] および [助詞-係助詞] の前に位置する「も」)
         */
        POS_TAG_SET.addTag(new PosTag("助詞-副助詞",Pos.AdverbialParticiple));
         /**
         *  particle-interjective: particles with interjective grammatical roles.
         *  e.g. (松島) や
         */
        POS_TAG_SET.addTag(new PosTag("助詞-間投助詞",Pos.Interjection,Pos.Particle));
         /**
         *  particle-coordinate:
         *  e.g. と, たり, だの, だり, とか, なり, や, やら
         */
        POS_TAG_SET.addTag(new PosTag("助詞-並立助詞",Pos.CoordinationParticle));
         /**
         *  particle-final:
         *  e.g. かい, かしら, さ, ぜ, (だ)っけ-口語/, (とまってる) で-方言/, な, ナ, なあ-口語/, ぞ, ね, ネ, 
         *       ねぇ-口語/, ねえ-口語/, ねん-方言/, の, のう-口語/, や, よ, ヨ, よぉ-口語/, わ, わい-口語/
         */
        POS_TAG_SET.addTag(new PosTag("助詞-終助詞",Pos.Particle));
         /**
         *  particle-adverbial/conjunctive/final: The particle "ka" when unknown whether it is 
         *  adverbial, conjunctive, or sentence final. For example:
         *       (a) 「A か B か」. Ex:「(国内で運用する) か,(海外で運用する) か (.)」
         *       (b) Inside an adverb phrase. Ex:「(幸いという) か (, 死者はいなかった.)」
         *           「(祈りが届いたせい) か (, 試験に合格した.)」
         *       (c) 「かのように」. Ex:「(何もなかった) か (のように振る舞った.)」
         *  e.g. か
         */
        POS_TAG_SET.addTag(new PosTag("助詞-副助詞／並立助詞／終助詞",Pos.AdverbialParticiple,Pos.ConjunctionPhrase));
         /**
         *  particle-adnominalizer: The "no" that attaches to nouns and modifies 
         *  non-inflectional words.
         */
        POS_TAG_SET.addTag(new PosTag("助詞-連体化",Pos.Particle));
         /**
         *  particle-adnominalizer: The "ni" and "to" that appear following nouns and adverbs 
         *  that are giongo, giseigo, or gitaigo.
         *  e.g. に, と
         */
        POS_TAG_SET.addTag(new PosTag("助詞-副詞化",Pos.Particle));
         /**
         *  particle-special: A particle that does not fit into one of the above classifications. 
         *  This includes particles that are used in Tanka, Haiku, and other poetry.
         *  e.g. かな, けむ, ( しただろう) に, (あんた) にゃ(わからん), (俺) ん (家)
         */
        POS_TAG_SET.addTag(new PosTag("助詞-特殊",Pos.Participle));
         /**
         *  auxiliary-verb:
         */
        POS_TAG_SET.addTag(new PosTag("助動詞",Pos.AuxiliaryVerb));
         /**
         *  interjection: Greetings and other exclamations.
         *  e.g. おはよう, おはようございます, こんにちは, こんばんは, ありがとう, どうもありがとう, ありがとうございます, 
         *       いただきます, ごちそうさま, さよなら, さようなら, はい, いいえ, ごめん, ごめんなさい
         */
        POS_TAG_SET.addTag(new PosTag("感動詞",Pos.Interjection));
         /**
         *  symbol: unclassified Symbols.
         */
        POS_TAG_SET.addTag(new PosTag("記号",Pos.Symbol));
         /**
         *  symbol-misc: A general symbol not in one of the categories below.
         *  e.g. [○◎@$〒→+]
         */
        POS_TAG_SET.addTag(new PosTag("記号-一般",Pos.Symbol));
        /**
        *  symbol-period: Periods and full stops.
        *  e.g. [.．。]
        */
       POS_TAG_SET.addTag(new PosTag("記号-句点",Pos.Point));
         /**
         *  symbol-comma: Commas
         *  e.g. [,、]
         */
        POS_TAG_SET.addTag(new PosTag("記号-読点",Pos.Comma));
         /**
         *  symbol-space: Full-width whitespace.
         */
        POS_TAG_SET.addTag(new PosTag("記号-空白",Pos.Symbol));
         /**
         *  symbol-open_bracket:
         *  e.g. [({‘“『【]
         */
        POS_TAG_SET.addTag(new PosTag("記号-括弧開",Pos.OpenBracket));
         /**
         *  symbol-close_bracket:
         *  e.g. [)}’”』」】]
         */
        POS_TAG_SET.addTag(new PosTag("記号-括弧閉",Pos.CloseBracket));
         /**
         *  symbol-alphabetic:
         */
        POS_TAG_SET.addTag(new PosTag("記号-アルファベット",Pos.Symbol));
         /**
         *  other: unclassified other
         */
        POS_TAG_SET.addTag(new PosTag("その他",Pos.Foreign));
         /**
         *  other-interjection: Words that are hard to classify as noun-suffixes or 
         *  sentence-final particles.
         *  e.g. (だ)ァ
         */
        POS_TAG_SET.addTag(new PosTag("その他-間投",LexicalCategory.Noun));
         /**
         *  filler: Aizuchi that occurs during a conversation or sounds inserted as filler.
         *  e.g. あの, うんと, えと
         */
        POS_TAG_SET.addTag(new PosTag("フィラー"));
         /**
         * * * * *
         *  non-verbal: non-verbal sound.
         */
        POS_TAG_SET.addTag(new PosTag("非言語音"));
         /**
         *  fragment:
         */
        POS_TAG_SET.addTag(new PosTag("語断片"));
         /**
         * * * * *
         *  unknown: unknown part of speech.
         */
        POS_TAG_SET.addTag(new PosTag("未知語",Pos.Foreign));
    }
}
