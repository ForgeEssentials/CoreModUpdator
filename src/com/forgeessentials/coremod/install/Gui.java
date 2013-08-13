package com.forgeessentials.coremod.install;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class Gui implements IHazOut
{
    private JFrame    frame;
    private JTextArea txtOut;
    private JButton   btnStable;
    private JButton   btnBeta;
    private JButton   btnDev;
    private JButton   btnClose;
    private JCheckBox chckbxAutoUpdate;
    
    /**
     * @wbp.parser.entryPoint
     */
    @Override
    public void init() throws Exception
    {
        this.frame = new JFrame();
        this.frame.setBounds(100, 100, 450, 300);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        this.frame.setTitle("FE Installer");
        
        this.txtOut = new JTextArea();
        this.frame.getContentPane().add(this.txtOut, BorderLayout.CENTER);
        
        final JPanel panel = new JPanel();
        this.frame.getContentPane().add(panel, BorderLayout.SOUTH);
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        
        this.btnStable = new JButton("Stable");
        this.btnStable.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                Main.setBranch("Stable");
            }
        });
        
        this.chckbxAutoUpdate = new JCheckBox("autoUpdate");
        this.chckbxAutoUpdate.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                Main.setAutoUpdate(Gui.this.chckbxAutoUpdate.isSelected());
            }
        });
        this.chckbxAutoUpdate.setSelected(true);
        panel.add(this.chckbxAutoUpdate);
        panel.add(this.btnStable);
        
        this.btnBeta = new JButton("Beta");
        this.btnBeta.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                Main.setBranch("Beta");
            }
        });
        panel.add(this.btnBeta);
        
        this.btnDev = new JButton("Dev");
        this.btnDev.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                Main.setBranch("Dev");
            }
        });
        panel.add(this.btnDev);
        
        this.btnClose = new JButton("Close");
        this.btnClose.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                System.exit(0);
            }
        });
        panel.add(this.btnClose);
        this.frame.setVisible(true);
    }
    
    @Override
    public void print(final Object o)
    {
        this.txtOut.setText(this.txtOut.getText() + o.toString());
    }
    
    @Override
    public void println(final Object o)
    {
        this.txtOut.setText(this.txtOut.getText() + o.toString() + "\n");
    }
    
    @Override
    public void stop()
    {
        this.btnStable.setEnabled(false);
        this.btnBeta.setEnabled(false);
        this.btnDev.setEnabled(false);
        this.chckbxAutoUpdate.setEnabled(false);
    }
}
