package io.prediction.algorithms.cascading.itemrec.popularrank;

import java.util.Properties;

import cascading.flow.Flow;
import cascading.flow.FlowDef;
import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.operation.aggregator.Count;
import cascading.operation.aggregator.First;
import cascading.operation.regex.RegexFilter;
import cascading.operation.Insert;
import cascading.pipe.Every;
import cascading.pipe.Each;
import cascading.pipe.GroupBy;
import cascading.pipe.HashJoin;
import cascading.pipe.Pipe;
import cascading.pipe.assembly.SumBy;
import cascading.pipe.assembly.Retain;
import cascading.pipe.assembly.Unique;
import cascading.pipe.assembly.Rename;
import cascading.pipe.joiner.LeftJoin;
import cascading.property.AppProps;
import cascading.scheme.Scheme;
import cascading.scheme.hadoop.TextDelimited;
import cascading.tap.Tap;
import cascading.tap.SinkMode;
import cascading.tap.hadoop.Hfs;
import cascading.tuple.Fields;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.ParseException;

/**
 * Example to demonstrate how to integrate custom algo.
 *
 * This examlpe algo simply recommends items based on popularity, which is determined by summing of all ratings.
 */
public class PopularRankAlgo {

  private static Option createRequiredArgOption(String optName, String argName, Object argType, String desc) {
    Option opt = OptionBuilder.withArgName(argName)
      .hasArg()
      .isRequired()
      .withType(argType)
      .withDescription(desc)
      .create(optName);

    return opt;
  }

  public static void main( String[] args ) {

    /* arguments */
    String ratingsPath = "";
    String itemRecScoresPath = "";
    int numRecommendations = 0;
    boolean unseenOnly = false;

    // example param
    int intParam = 0;
    double doubleParam = 0;
    String stringParam = "";

    Options options = new Options();

    Option inputOpt = createRequiredArgOption("input", "input file", String.class, 
      "Input file path in tsv (tab delimited) format (uid iid rating).");
    Option outputOpt = createRequiredArgOption("output", "input file", String.class,
      "Output file path in tsv (tab delimited) format (uid iid score).");
    Option numRecommendationsOpt = createRequiredArgOption("numRecommendations", "Int", Number.class,
      "Number of recommendations.");
    Option unseenOnlyOpt = createRequiredArgOption("unseenOnly", "true/false", String.class,
      "Recommend unseen items only.");
    Option intParamOpt = createRequiredArgOption("intParam", "Int", Number.class,
      "Integer param example.");
    Option doubleParamOpt = createRequiredArgOption("doubleParam", "Double", Number.class,
      "Double param example.");
    Option stringParamOpt = createRequiredArgOption("stringParam", "String", String.class,
      "String param example.");

    options.addOption( inputOpt );
    options.addOption( outputOpt );
    options.addOption( numRecommendationsOpt );
    options.addOption( unseenOnlyOpt );
    options.addOption( intParamOpt );
    options.addOption( doubleParamOpt );
    options.addOption( stringParamOpt );

    CommandLineParser parser = new BasicParser();
    try {
      CommandLine line = parser.parse( options, args );

      ratingsPath = (String) line.getParsedOptionValue( "input" );
      itemRecScoresPath = (String) line.getParsedOptionValue( "output" );
      numRecommendations = ((Number) line.getParsedOptionValue( "numRecommendations" )).intValue();
      unseenOnly = Boolean.parseBoolean((String) line.getParsedOptionValue( "unseenOnly" ));
      // example param
      intParam = ((Number) line.getParsedOptionValue( "intParam" )).intValue();
      doubleParam = ((Number) line.getParsedOptionValue( "doubleParam" )).doubleValue();
      stringParam = (String) line.getParsedOptionValue( "stringParam" );

      System.out.println("Going to run the job with following arguments...");
      System.out.println("input: " + ratingsPath);
      System.out.println("output: " + itemRecScoresPath);
      System.out.println("numRecommendations: " + numRecommendations);
      System.out.println("unseenOnly: " + unseenOnly);
      System.out.println("intParam: " + intParam);
      System.out.println("doubleParam: " + doubleParam);
      System.out.println("stringParam: " + stringParam);
      
    } catch( Exception exp ) {
      System.err.println( "Invalid arguments: " + exp.getMessage() );
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp( "arguments", options );

      System.exit(1);
    }

    /* cascading job */
    Properties properties = new Properties();
    AppProps.setApplicationJarClass( properties, PopularRankAlgo.class );
    HadoopFlowConnector flowConnector = new HadoopFlowConnector( properties );

    Fields ratingsFields = new Fields( "uid", "iid", "rating" );
    Fields uid = new Fields( "uid" );
    Fields iid = new Fields( "iid" );
    Fields rating = new Fields( "rating" );
    Fields score = new Fields( "score" );
    Fields lhs_join = new Fields( "lhs_join" );
    Fields rhs_join = new Fields( "rhs_join" );
    Fields itemRecScoresFields = new Fields( "uid", "iid", "score" );

    Tap ratingsTap = new Hfs( new TextDelimited( ratingsFields, "\t" ), ratingsPath );
    Tap itemRecScoresTap = new Hfs( new TextDelimited( itemRecScoresFields, "\t" ), itemRecScoresPath, SinkMode.REPLACE );
    //Mode REPLACE will delete/remove the resource before any attempts to write.
    //Tap tempTap = new Hfs( new TextDelimited( true, "\t" ), itemRecScoresPath, SinkMode.REPLACE );
    
    Pipe ratingsPipe = new Pipe("ratingsPipe");

    // get all uid from ratings
    Pipe userPipe = new Pipe( "user", ratingsPipe );
    userPipe = new Unique( userPipe, uid );
    userPipe = new Retain(userPipe, uid );
    userPipe = new Each( userPipe, new Insert( rhs_join, 1 ), Fields.ALL );

    // add all rating for each item to determin popularity
    Pipe popPipe = new Pipe( "pop", ratingsPipe );
    popPipe = new SumBy( popPipe, iid, rating, score, long.class );
    popPipe = new Each( popPipe, new Insert( lhs_join, 1 ), Fields.ALL );

    // typical application has less user than item
    Pipe scorePipe = new HashJoin( "score", popPipe, lhs_join, userPipe, rhs_join );
    scorePipe = new Retain( scorePipe, itemRecScoresFields );

    if (unseenOnly) {
      // left join with rating again, keep those raings==0 (unseenOnly)
      Fields uid_iid = new Fields( "uid", "iid" );
      Fields uidR_iidR = new Fields( "uidR", "iidR" );

      // if there are any actions for this uid_iid, it means the items have been seen.
      // uniquify the ratingsPipe by uid_iid in case there are multiple actions on same items by the same user.
      Pipe uniqueRatingsPipe = new Pipe( "uniqueRatingsPipe", ratingsPipe );
      uniqueRatingsPipe = new Unique( uniqueRatingsPipe, uid_iid );
      uniqueRatingsPipe = new Rename( uniqueRatingsPipe, uid_iid, uidR_iidR );

      scorePipe = new HashJoin( "unseen", scorePipe, uid_iid, uniqueRatingsPipe, uidR_iidR, new LeftJoin() );
      scorePipe = new Each( scorePipe, rating, new RegexFilter( "^$" ) );
      scorePipe = new Retain( scorePipe, itemRecScoresFields );
    }

    // sort by score for each uid
    Pipe recPipe = new Pipe( "rec", scorePipe );
    // group by uid, sort by score (reverse sort = true)
    recPipe = new GroupBy( recPipe, uid, score, true );
    // only take first numRecommendations for each uid
    recPipe = new Every( recPipe, new First(numRecommendations), Fields.RESULTS );

    FlowDef flowDef = FlowDef.flowDef()
      .setName( "CustomAlgo" )
      .addSource( ratingsPipe, ratingsTap )
      .addTailSink( recPipe, itemRecScoresTap );
      //.addTailSink( recPipe, tempTap );

    // write a DOT file and run the flow
    Flow wcFlow = flowConnector.connect( flowDef );
    //wcFlow.writeDOT( "dot/wc.dot" );
    wcFlow.complete();

  }
}
