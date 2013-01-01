package name.wadewalker.convertflash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//==============================================================================
/**
 * Uses ffmpeg to convert flash videos to WMV video.
 *
 * Copyright (c) 2011 Wade Walker. All rights reserved.
 * @author Wade Walker
 */
public class ConvertFlash {

    /** Number of milliseconds to wait between checks for finished processes. */
    private static final int siMSBetweenProcessChecks = 1000;

    /** Directory to get Flash vids from. */
//    private String sInputDir = "D:\\Users\\Wade\\Videos\\Music Videos\\New";
    private String sInputDir = "D:\\Users\\Wade\\My Pictures\\2012-06-23 through 08-13 Chess";

    /** Directory to put WMV files. */
//    private String sOutputDir = "D:\\Users\\Wade\\Videos\\Music Videos\\Converted";
//    private String sOutputDir = "D:\\Users\\Wade\\Videos\\Music Videos\\New";
    private String sOutputDir = "D:\\Users\\Wade\\My Pictures\\2012-06-23 through 08-13 Chess";

    /** ffmpeg binary path. */
    private String sConverterPath = "D:\\Users\\Wade\\Videos\\Music Videos\\ffmpeg-git-81ef892-win64-static\\bin\\ffmpeg.exe";

    /** Max processes to run at once. */
    private int iMaxProcesses = 4;

    /**
     * Consumes the output of a process. Needed to keep processes from hanging on startup.
     * Use one for a process' output stream, and one for its error stream.
     */
    class StreamReader extends Thread {
        /** Stream to read from. */
        private InputStream inputstream;

        //==============================================================================
        /**
         * Constructor.
         * @param inputstreamParam Stream to read from.
         */
        StreamReader( InputStream inputstreamParam ) {
            inputstream = inputstreamParam;
        }

        //==============================================================================
        /**
         * Consumes the stream's output until the reader returns null.
         */
        public void run() {
            try {
                BufferedReader bufferedreader = new BufferedReader( new InputStreamReader( inputstream ) );
                String sLine = null;
                while( (sLine = bufferedreader.readLine()) != null )
                    System.out.println( sLine );
            }
            catch( IOException ioexception ) {
                ioexception.printStackTrace();
            }
        }
    }

    //==============================================================================
    /**
     * A thread that converts one file.
     */
    class Converter extends Thread {
        /** Set true when the conversion is done. */
        private boolean bDone;

        /** Command and arguments for one file conversion. */
        private List<String> listCommandAndArgs;


        //==============================================================================
        /**
         * Constructor.
         * @param listCommandAndArgsParam {@link #listCommandAndArgs}
         */
        Converter( List<String> listCommandAndArgsParam ) {
            listCommandAndArgs = listCommandAndArgsParam;
        }

        //==============================================================================
        /**
         * Runs ffmpeg on one file.
         */
        public void run() {
            Process process = null;
            try {
                process = Runtime.getRuntime().exec( listCommandAndArgs.toArray( new String [] {} ) );
            }
            catch( IOException ioexception ) {
                System.err.print( String.format( "Can't start process {0}\n", listCommandAndArgs.get( 0 ) ) );
                ioexception.printStackTrace();
            }

            StreamReader streamreaderError = new StreamReader( process.getErrorStream() );
            StreamReader streamreaderOutput = new StreamReader( process.getInputStream() );
            streamreaderError.start();
            streamreaderOutput.start();

            int iExitVal = 0;
            try {
                // wait for process to quit and return the return code
                iExitVal = process.waitFor();
                System.out.println( "Exit value: " + iExitVal );
            }
            catch( InterruptedException interruptedexception ) {
                // ignore, because if the process has been interrupted, we don't need to wait for it
            }

            bDone = true;
        }

        //==============================================================================
        /**
         * Accessor.
         * @return {@link #bDone}
         */
        public boolean isDone() {
            return( bDone );
        }
    }

    //==============================================================================
    /**
     * Constructor.
     */
    public ConvertFlash() {
    }

    //==============================================================================
    /**
     * Converts all .flv files in the input directory to .wmv files in the output directory.
     */
    public void convert() {
        // check input directory existence
        File fileInputDir = new File( sInputDir );
        if( !fileInputDir.exists() ) {
            System.err.print( String.format( "The input directory {0} doesn't exist.\n", sInputDir ) );
            return;
        }

        // check output directory existence
        File fileOutputDir = new File( sOutputDir );
        if( !fileOutputDir.exists() ) {
            System.err.print( String.format( "The WMV output directory {0} doesn't exist.\n", sOutputDir ) );
            return;
        }

        // check ffmpeg existence
        File fileConverter = new File( sConverterPath );
        if( !fileConverter.exists() ) {
            System.err.print( String.format( "The ffmpeg binary {0} doesn't exist.\n", sConverterPath ) );
            return;
        }

        // get all Flash files
        FilenameFilter filenamefilterFlash = new FilenameFilter() {
            public boolean accept( File fileDir, String sName ) {
                return sName.endsWith( ".flv" );
            }
        };

        String [] asFlashFiles = fileInputDir.list( filenamefilterFlash );

        String [] asMP4Files = new String [0];    // Directory.GetFiles( sInputDir, "*.mp4" );
        String [] asInputFiles = new String [asFlashFiles.length + asMP4Files.length];
        System.arraycopy( asFlashFiles, 0, asInputFiles, 0, asFlashFiles.length );
        System.arraycopy( asMP4Files, 0, asInputFiles, asFlashFiles.length, asMP4Files.length );

        Pattern patternFileName = Pattern.compile( "(.+?)\\s+-\\s+(.+)\\.(?:flv|mp4)" );

        // create a process command line for each file
        List<List<String>> listUnstartedProcesses = new ArrayList<List<String>>();
        for( String sInputFile : asInputFiles ) {

            Matcher matcher = patternFileName.matcher( sInputFile );

            // skip files that don't match
            if( !matcher.matches() )
                continue;

            String sArtist = matcher.group( 1 ).trim();
            String sTitle = matcher.group( 2 ).trim();

            // command and arguments for converter
            List<String> listCmdAndArgs = new ArrayList<String>();
            listCmdAndArgs.add( "\"" + sConverterPath + "\"" );
            listCmdAndArgs.add( "-i" );
            listCmdAndArgs.add( "\"" + sInputDir + "\\" + sInputFile + "\"" );
            listCmdAndArgs.add( "-b" );
            listCmdAndArgs.add( "1200k" );
//            listCmdAndArgs.add( "\"" + sOutputDir + "\\" + sArtist + " - " + sTitle + ".wmv\"" );
            listCmdAndArgs.add( "\"" + sOutputDir + "\\" + sArtist + " - " + sTitle + ".mp4\"" );

            listUnstartedProcesses.add( listCmdAndArgs );
        }

        // run conversion processes concurrently
        List<Converter> listRunningProcesses = new ArrayList<Converter>();
        while( (listUnstartedProcesses.size() > 0) || (listRunningProcesses.size() > 0) ) {

            // don't spin wait
            try {
                Thread.sleep( siMSBetweenProcessChecks );
            }
            catch( InterruptedException interruptedexception ) {
                // just continue if interrupted
            }

            // remove any processes that are done
            int i = 0;
            while( i < listRunningProcesses.size() ) {
                Converter converter = listRunningProcesses.get( i );
                if( converter.isDone() )
                    listRunningProcesses.remove( i );
                else
                    i++;
            }

            // start new processes (up to max)
            while( (listRunningProcesses.size() < iMaxProcesses) && (listUnstartedProcesses.size() > 0) ) {
                List<String> listCmdAndArgs = listUnstartedProcesses.get( 0 );
                listUnstartedProcesses.remove( 0 );
                Converter converter = new Converter( listCmdAndArgs );
                converter.start();
                listRunningProcesses.add( converter );
            }
        }
    }

    //==============================================================================
    /**
     * The main entry point for the application.
     * @param asArgs Command line arguments (ignored).
     */
    public static void main( String [] asArgs ) {
        ConvertFlash convertflash = new ConvertFlash();
        convertflash.convert();
    }
}