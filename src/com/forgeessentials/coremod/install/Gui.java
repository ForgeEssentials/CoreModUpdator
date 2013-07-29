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
        frame = new JFrame();
        frame.setBounds(100, 100, 450, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setTitle("FE Installer");
        
        txtOut = new JTextArea();
        frame.getContentPane().add(txtOut, BorderLayout.CENTER);
        
        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.SOUTH);
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        
        btnStable = new JButton("Stable");
        btnStable.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                Main.setBranch("Stable");
            }
        });
        
        chckbxAutoUpdate = new JCheckBox("autoUpdate");
        chckbxAutoUpdate.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                Main.setAutoUpdate(chckbxAutoUpdate.isSelected());
            }
        });
        chckbxAutoUpdate.setSelected(true);
        panel.add(chckbxAutoUpdate);
        panel.add(btnStable);
        
        btnBeta = new JButton("Beta");
        btnBeta.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                Main.setBranch("Beta");
            }
        });
        panel.add(btnBeta);
        
        btnDev = new JButton("Dev");
        btnDev.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                Main.setBranch("Dev");
            }
        });
        panel.add(btnDev);
        
        btnClose = new JButton("Close");
        btnClose.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                System.exit(0);
            }
        });
        panel.add(btnClose);
        frame.setVisible(true);
    }
    
    @Override
    public void print(Object o)
    {
        txtOut.setText(txtOut.getText() + o.toString());
    }
    
    @Override
    public void println(Object o)
    {
        txtOut.setText(txtOut.getText() + o.toString() + "\n");
    }
    
    @Override
    public void stop()
    {
        btnStable.setEnabled(false);
        btnBeta.setEnabled(false);
        btnDev.setEnabled(false);
        chckbxAutoUpdate.setEnabled(false);
    }
}
