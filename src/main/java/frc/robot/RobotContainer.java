package frc.robot;


import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PneumaticHub;
import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.PowerDistribution.ModuleType;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.lib.util.States.GamePiece;
import frc.robot.Arm.Arm;
import frc.robot.Arm.NamedPose;
import frc.robot.Arm.command.ArmCommand;
import frc.robot.Shooter.Intake;
import frc.robot.Shooter.IntakeConfig;
import frc.robot.Shooter.Shooter;
import frc.robot.Shooter.Commands.ShootPiece;
import frc.robot.Shooter.Commands.SpinShooter;
import frc.robot.Shooter.Commands.TeleopIntake;
import frc.robot.Shooter.Commands.TeleopOuttake;
import frc.robot.autos.AutoSelector;
import frc.robot.commands.Aim;
import frc.robot.subsystems.Leds;
import frc.robot.subsystems.Limelight;
import frc.robot.subsystems.Thumbwheel;
import frc.robot.swerve.Swerve;
import frc.robot.swerve.command.LockSwerveCommand;
import frc.robot.swerve.command.TeleopSwerve;

/**
 * This class is where the bulk of the robot should be declared. Since
 * Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in
 * the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of
 * the robot (including
 * subsystems, commands, and button mappings) should be declared here.
 */
public class RobotContainer 
{
    /* Controllers */
    private final Joystick driveStick;
    private final Joystick rotateStick;
    private final XboxController controller;
    private final GenericHID buttonBoard;
    //private Limelight blindingDevice;

    /* Drive Controls */
    private final int translationAxis;
    private final int strafeAxis;
    private final int rotationAxis;

    /*
     * private final int translationAxis = XboxController.Axis.kLeftY.value;
     * private final int strafeAxis = XboxController.Axis.kLeftX.value;
     * private final int rotationAxis = XboxController.Axis.kRightX.value;
     */

    /* Driver Buttons */ // TODO get rid of
    private final Trigger zeroGyro;
    private final Trigger robotCentric;


    /* Subsystems */
    private final Swerve s_Swerve;
    private final Arm s_Arm;
    private final Shooter s_Shooter;
    private final Thumbwheel s_Thumb;
    private final Leds s_leds;



    private PowerDistribution powerBoard;
    private PieceMode pieceMode;
    // TODO uncomment once intake exists
     private final Intake s_Intake;

    /** The container for the robot. Contains subsystems, OI devices, and commands. */
    public RobotContainer() 
    {

        //blindingDevice = new Limelight();
        powerBoard = new PowerDistribution(20, ModuleType.kRev);
        PneumaticHub ph = new PneumaticHub(Constants.REV.PHID);
        CameraServer.startAutomaticCapture();

        /* Controllers */
        driveStick = new Joystick(0);
        rotateStick = new Joystick(1);

        controller = new XboxController(2);
        buttonBoard = new GenericHID(3);


     

        /* Drive Controls */
        translationAxis = Joystick.AxisType.kY.value;
        strafeAxis = Joystick.AxisType.kX.value;
        rotationAxis = Joystick.AxisType.kX.value;

        /* Driver Buttons */
        zeroGyro = new Trigger(()->driveStick.getRawButton(1));
        robotCentric = new Trigger(()-> driveStick.getRawButton(2));

        /* Subsystems */
        s_Swerve = new Swerve();
        s_Thumb = new Thumbwheel();

     
        pieceMode = new PieceMode();
        s_Arm = new Arm(NamedPose.Home, ph, pieceMode);
        s_Intake = new Intake(ph);
        s_Shooter = new Shooter();
       
        s_leds = new Leds(pieceMode);
   
        Command c;
        s_Swerve.setDefaultCommand(
            new TeleopSwerve(
                s_Swerve, 
                () -> Math.pow(driveStick.getRawAxis(translationAxis),3), 
                () -> Math.pow(driveStick.getRawAxis(strafeAxis),3), 
                () -> rotateStick.getRawAxis(rotationAxis)/2, 
                () -> robotCentric.getAsBoolean()
            )
        );
       // s_leds.setDefaultCommand(new ledControlCommand(s_leds, rotateStick));
        
        // Configure the button bindings
        configureButtonBindings();
        
    }


   
    /**
     * Use this method to define your button->command mappings. Buttons can be
     * created by
     * instantiating a {@link GenericHID} or one of its subclasses ({@link
     * edu.wpi.first.wpilibj.Joystick} or {@link XboxController}), and then passing
     * it to a {@link
     * edu.wpi.first.wpilibj2.command.button.JoystickButton}.
     */
    private void configureButtonBindings() {

        /* Driver Buttons */
        zeroGyro.onTrue(new InstantCommand(() -> s_Swerve.zeroGyro()));

        Trigger lockSwerve = new Trigger(() -> rotateStick.getRawButton(1));
        lockSwerve.onTrue(new LockSwerveCommand(s_Swerve, ()->!lockSwerve.getAsBoolean()));
        



        Trigger oneEightyGryo = new Trigger(() -> rotateStick.getRawButton(3));
        oneEightyGryo.onTrue(new InstantCommand(()->s_Swerve.zeroGyro(180)));
        //Trigger aim = new Trigger(() -> rotateStick.getRawButton(3));
        //aim.onTrue(new Aim(s_Swerve, blindingDevice));

        Trigger shootCube = new Trigger(()-> rotateStick.getRawButton(7));
        shootCube.whileTrue(new ShootPiece(s_Intake, s_Shooter));
     

        Trigger closeClaw = new Trigger(() -> driveStick.getRawButton(10));
        closeClaw.onTrue(new InstantCommand(()->s_Arm.setClaw(true)));

        Trigger openClaw= new Trigger(() -> driveStick.getRawButton(11));
        openClaw.onTrue(new InstantCommand(()->s_Arm.setClaw(false)));

    

        // Button board (this is terrible)
        Trigger BB1 = new Trigger(() -> buttonBoard.getRawButton(1));
        BB1.onTrue(new InstantCommand(()->ArmCommand.PlotPathAndSchedule( NamedPose.PouncePreScore, s_Arm,powerBoard)));

        Trigger BB2 = new Trigger(() -> buttonBoard.getRawButton(2));
        BB2.onTrue(new InstantCommand(()->ArmCommand.PlotPathAndSchedule( NamedPose.ScoreL1, s_Arm,powerBoard)));

        Trigger BB3 = new Trigger(() -> buttonBoard.getRawButton(7));
        BB3.onTrue(new InstantCommand(()->ArmCommand.PlotPathAndSchedule( NamedPose.ScoreL2, s_Arm,powerBoard)));

        Trigger BB4 = new Trigger(() -> buttonBoard.getRawButton(4));
        BB4.onTrue(new InstantCommand(()->ArmCommand.PlotPathAndSchedule( NamedPose.ScoreL3, s_Arm,powerBoard)));


        Trigger BB5 = new Trigger(() -> buttonBoard.getRawButton(5));
        BB5.onTrue(new InstantCommand(()->{pieceMode.setPiece(GamePiece.cone);}));

        Trigger BB6 = new Trigger(() -> buttonBoard.getRawButton(6));
        BB6.onTrue(new InstantCommand(()->{pieceMode.setPiece(GamePiece.cube);}));


        Trigger BB7 = new Trigger(() -> buttonBoard.getRawButton(3));
        BB7.onTrue(new InstantCommand(()->ArmCommand.PlotPathAndSchedule( NamedPose.FloorPick, s_Arm,powerBoard)));

        Trigger BB8 = new Trigger(() -> buttonBoard.getRawButton(8));
        BB8.onTrue(new InstantCommand(()->ArmCommand.PlotPathAndSchedule( NamedPose.PounceDriveUpWindow, s_Arm,powerBoard)));

        Trigger BB9 = new Trigger(() -> buttonBoard.getRawButton(9));
        BB9.onTrue(new InstantCommand(()->ArmCommand.PlotPathAndSchedule( NamedPose.PickDriveUpWindow, s_Arm,powerBoard)));

        Trigger BB10 = new Trigger(() -> buttonBoard.getRawButton(10));
        BB10.onTrue(new InstantCommand(()->
        {
            ArmCommand.PlotPathAndSchedule( NamedPose.PickFromSubstation, s_Arm);
            powerBoard.setSwitchableChannel(true); // TODO test code - make a proper solution!
        }
        ));

        Trigger home = new Trigger(() -> buttonBoard.getRawButton(11));
        home.onTrue(new InstantCommand(()->ArmCommand.PlotPathAndSchedule( NamedPose.Home, s_Arm,powerBoard)));

        new Trigger(() -> buttonBoard.getRawButton(12))
            .onTrue(new InstantCommand(()->ArmCommand.PlotPathAndSchedule( NamedPose.Travel, s_Arm,powerBoard)));


        // Xbox controller



        Trigger resetOdometrey = new Trigger(()-> rotateStick.getRawButton(2));
        resetOdometrey.onTrue(new InstantCommand(()-> {s_Swerve.resetOdometry(new Pose2d(   /*wow*/));}));//this is normal

        // Xbox controller

        Trigger intakeButton = new Trigger(()-> controller.getRightBumper());
        intakeButton.whileTrue(new TeleopIntake(s_Intake));

        Trigger outtakeButton = new Trigger(()-> controller.getLeftBumper());
        outtakeButton.whileTrue(new TeleopOuttake(s_Intake));

        Trigger shootL1 = new Trigger(()-> controller.getAButton());
        shootL1.whileTrue(new SpinShooter(s_Shooter, IntakeConfig.level1Speed));

        Trigger shootL2 = new Trigger(()-> controller.getXButton());
        shootL2.whileTrue(new SpinShooter(s_Shooter, IntakeConfig.level2Speed));

        Trigger shootL3 = new Trigger(()-> controller.getYButton());
        shootL3.whileTrue(new SpinShooter(s_Shooter, IntakeConfig.level3Speed));
        
    }


    /*// excuse me for this hack
    private InstantCommand shootCube()
    {
        return new InstantCommand(()->{
       
        s_Intake.shoot();});
    }

    private InstantCommand stopShooting()
    {
        return new InstantCommand(()->{
   
            s_Intake.stopIntake();
        });
    }*/

    /**
     * Use this to pass the autonomous command to the main {@link Robot} class.
     *
     * @return the command to run in autonomous
     */
    public Command getAutonomousCommand() {
        // An ExampleCommand will run in autonomous
        AutoSelector auto =new AutoSelector(s_Thumb, s_Arm, s_Swerve, pieceMode);
        return auto.getAutonomousCommand();
    }
}
