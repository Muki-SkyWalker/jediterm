package com.jediterm.terminal.ui;

import com.google.common.base.Predicate;
import com.jediterm.terminal.RequestOrigin;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.debug.BufferPanel;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;

import com.jediterm.terminal.ui.settings.DefaultTabbedSettingsProvider;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public abstract class AbstractTerminalFrame {
  public static final Logger LOG = Logger.getLogger(AbstractTerminalFrame.class);

  private JFrame myBufferFrame;

  private TerminalWidget myTerminal;
  
  private AbstractAction myOpenAction = new AbstractAction("New Session") {
    public void actionPerformed(final ActionEvent e) {
      openSession(myTerminal);
    }
  };

  private AbstractAction myResetDamage = new AbstractAction("Reset damage") {
    public void actionPerformed(final ActionEvent e) {
      myTerminal.getCurrentSession().getBackBuffer().resetDamage();
    }
  };

  private AbstractAction myDrawDamage = new AbstractAction("Draw from damage") {
    public void actionPerformed(final ActionEvent e) {
      myTerminal.getCurrentSession().redraw();
    }
  };

  private AbstractAction myShowBuffersAction = new AbstractAction("Show buffers") {
    public void actionPerformed(final ActionEvent e) {
      if (myBufferFrame == null) {
        showBuffers();
      }
    }
  };

  private AbstractAction myDumpDimension = new AbstractAction("Dump terminal dimension") {
    public void actionPerformed(final ActionEvent e) {
      LOG.info(myTerminal.getTerminalDisplay().getColumnCount() +
          "x" + myTerminal.getTerminalDisplay().getRowCount());
    }
  };
  
  private AbstractAction myDumpCursorPosition = new AbstractAction("Dump cursor position") {
    public void actionPerformed(final ActionEvent e) {
      LOG.info(myTerminal.getCurrentSession().getTerminal().getCursorX() +
          "x" + myTerminal.getCurrentSession().getTerminal().getCursorY());
    }
  };

  private AbstractAction myCursor0x0 = new AbstractAction("1x1") {
    public void actionPerformed(final ActionEvent e) {
         myTerminal.getCurrentSession().getTerminal().cursorPosition(1, 1);
    }
  };

  private AbstractAction myCursor10x10 = new AbstractAction("10x10") {
    public void actionPerformed(final ActionEvent e) {
         myTerminal.getCurrentSession().getTerminal().cursorPosition(10, 10);
    }
  };

  private AbstractAction myCursor80x24 = new AbstractAction("80x24") {
    public void actionPerformed(final ActionEvent e) {
         myTerminal.getCurrentSession().getTerminal().cursorPosition(80, 24);
    }
  };

  private JMenuBar getJMenuBar() {
    final JMenuBar mb = new JMenuBar();
    final JMenu m = new JMenu("File");

    m.add(myOpenAction);
    mb.add(m);
    final JMenu dm = new JMenu("Debug");

    dm.add(myShowBuffersAction);
    dm.add(myResetDamage);
    dm.add(myDrawDamage);
    dm.addSeparator();
    dm.add(myDumpDimension);
    dm.add(myDumpCursorPosition);
    
    JMenu cursorPosition = new JMenu("Set cursor position ...");
    cursorPosition.add(myCursor0x0);
    cursorPosition.add(myCursor10x10);
    cursorPosition.add(myCursor80x24);
    dm.add(cursorPosition);
    mb.add(dm);

    return mb;
  }

  private void openSession(TerminalWidget terminal) {
    if (terminal.canOpenSession()) {
      openSession(terminal, createTtyConnector());
    }
  }

  public void openSession(TerminalWidget terminal, TtyConnector ttyConnector) {
    TerminalSession session = terminal.createTerminalSession(ttyConnector);
    session.start();
  }

  public abstract TtyConnector createTtyConnector();

  protected AbstractTerminalFrame() {
    myTerminal = new TabbedTerminalWidget(new DefaultTabbedSettingsProvider(), new Predicate<TerminalWidget>() {
      @Override
      public boolean apply(TerminalWidget terminalWidget) {
        openSession(terminalWidget);
        return true;
      }
    });

    final JFrame frame = new JFrame("JediTerm");

    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        System.exit(0);
      }
    });

    final JMenuBar mb = getJMenuBar();
    frame.setJMenuBar(mb);
    sizeFrameForTerm(frame);
    frame.getContentPane().add("Center", myTerminal.getComponent());

    frame.pack();
    frame.setVisible(true);

    frame.setResizable(true);

    myTerminal.setTerminalPanelListener(new TerminalPanelListener() {
      public void onPanelResize(final Dimension pixelDimension, final RequestOrigin origin) {
        if (origin == RequestOrigin.Remote) {
          sizeFrameForTerm(frame);
        }
        frame.pack();
      }

      @Override
      public void onSessionChanged(final TerminalSession currentSession) {
        frame.setTitle(currentSession.getSessionName());
      }

      @Override
      public void onTitleChanged(String title) {
        frame.setTitle(myTerminal.getCurrentSession().getSessionName());
      }
    });

    openSession(myTerminal);
  }

  private void sizeFrameForTerm(final JFrame frame) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Dimension d = myTerminal.getPreferredSize();

        d.width += frame.getWidth() - frame.getContentPane().getWidth();
        d.height += frame.getHeight() - frame.getContentPane().getHeight();
        frame.setSize(d);
      }
    });
  }

  private void showBuffers() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        myBufferFrame = new JFrame("buffers");
        final JPanel panel = new BufferPanel(myTerminal.getCurrentSession());

        myBufferFrame.getContentPane().add(panel);
        myBufferFrame.pack();
        myBufferFrame.setVisible(true);
        myBufferFrame.setSize(800, 600);

        myBufferFrame.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(final WindowEvent e) {
            myBufferFrame = null;
          }
        });
      }
    });
  }

}
