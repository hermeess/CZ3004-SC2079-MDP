/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.c
  * @brief          : Main program body
  ******************************************************************************
  * @attention
  *
  * Copyright (c) 2023 STMicroelectronics.
  * All rights reserved.
  *
  * This software is licensed under terms that can be found in the LICENSE file
  * in the root directory of this software component.
  * If no LICENSE file comes with this software, it is provided AS-IS.
  *
  ******************************************************************************
  */
/* USER CODE END Header */
/* Includes ------------------------------------------------------------------*/
#include "main.h"
#include "cmsis_os.h"
#include <math.h>
/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include "oled.h"
#include "oled.c"
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */
int diffA = 0;
int diffB = 0;
int targetSpeed = 0;
int targetRotations = 0;
uint32_t IC_Val1 = 0;
uint32_t IC_Val2 = 0;
uint32_t Difference = 0;
uint8_t Is_First_Captured = 0;
uint8_t Distance = 0;
int IRLeft = 0;
int IRRight = 0;

//Gyroscope variables
double total_angle = 0;
double ideal_angle = 0;
uint8_t gyroBuffer[20];
uint8_t ICMAddress = 0x68;
int notdone=1;
uint8_t aRxBuffer[20];
int update = 0;
//43
uint32_t leftTurnOffset = 52;
uint32_t leftBackTurnOffset = 55;
//61
uint32_t rightTurnOffset = 78;
uint32_t rightBackTurnOffset = 71;
int stopMargin = 160;
uint16_t maxPWMval = 5000; //7199
uint16_t minPWMval = 3500;
uint32_t wheelStraight = 150;
uint32_t turnPWMval = 2400;
uint32_t turnPWMvalBack = 2200;
double degreeOffset = 2;
double degreeOffsetBack = 2;
int breakDelay = 1700;
int accelRate = 300;

char globalMessages[30];
int obDist1 = 0;
int obDist2 = 0;
int obDist2Pure = 0;
int capobDist1 = 0;
int capobDist2 = 0;
int obWidth2 = 0;
int forwardComp = 80;

#define TRIG_PIN GPIO_PIN_9
#define TRIG_PORT GPIOD
/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/
ADC_HandleTypeDef hadc1;
ADC_HandleTypeDef hadc2;

I2C_HandleTypeDef hi2c1;

TIM_HandleTypeDef htim1;
TIM_HandleTypeDef htim2;
TIM_HandleTypeDef htim3;
TIM_HandleTypeDef htim4;
TIM_HandleTypeDef htim8;

UART_HandleTypeDef huart3;

/* Definitions for UARTask */
osThreadId_t UARTaskHandle;
const osThreadAttr_t UARTask_attributes = {
  .name = "UARTask",
  .stack_size = 128 * 12,
  .priority = (osPriority_t) osPriorityNormal,
};
/* Definitions for showOled */
osThreadId_t showOledHandle;
const osThreadAttr_t showOled_attributes = {
  .name = "showOled",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityLow,
};
/* Definitions for Ultrasonic */
osThreadId_t UltrasonicHandle;
const osThreadAttr_t Ultrasonic_attributes = {
  .name = "Ultrasonic",
  .stack_size = 128 * 8,
  .priority = (osPriority_t) osPriorityLow,
};
/* Definitions for gyro */
osThreadId_t gyroHandle;
const osThreadAttr_t gyro_attributes = {
  .name = "gyro",
  .stack_size = 128 * 8,
  .priority = (osPriority_t) osPriorityLow,
};
/* Definitions for IRtask */
osThreadId_t IRtaskHandle;
const osThreadAttr_t IRtask_attributes = {
  .name = "IRtask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityLow,
};
/* USER CODE BEGIN PV */

/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
void SystemClock_Config(void);
static void MX_GPIO_Init(void);
static void MX_TIM8_Init(void);
static void MX_TIM2_Init(void);
static void MX_TIM1_Init(void);
static void MX_USART3_UART_Init(void);
static void MX_TIM3_Init(void);
static void MX_TIM4_Init(void);
static void MX_I2C1_Init(void);
static void MX_ADC1_Init(void);
static void MX_ADC2_Init(void);
void UartTask(void *argument);
void show(void *argument);
void ultrasonicTask(void *argument);
void GyroTask(void *argument);
void IRTask(void *argument);

static void MX_NVIC_Init(void);
/* USER CODE BEGIN PFP */

/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */

void motorsInit(){
	  HAL_TIM_PWM_Start(&htim1, TIM_CHANNEL_4);
	  HAL_TIM_PWM_Start(&htim8, TIM_CHANNEL_1);
	  HAL_TIM_PWM_Start(&htim8, TIM_CHANNEL_2);

	  //Encoder: 1559 seems to be one rev
	  HAL_TIM_Encoder_Start(&htim2, TIM_CHANNEL_ALL);
	  HAL_TIM_Encoder_Start(&htim3, TIM_CHANNEL_ALL);
}
void goForwardA(uint16_t pwmVal){

	  HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_SET);
	  HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_RESET);
	  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmVal);
}

void goForwardB(uint16_t pwmVal){
	  HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_SET);
	  HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_RESET);
	  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmVal);
}

void goBackA(uint16_t pwmVal){
	  HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_RESET);
	  HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_SET);
	  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmVal);
}

void goBackB(uint16_t pwmVal){
	  HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_RESET);
	  HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_SET);
	  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmVal);
}

void goForwardDist(int cmDist){
	  uint16_t pwmValA = 4000;
	  uint16_t pwmValB = 4000;
	  unsigned int AChange = 0;
	  unsigned int BChange = 0;
	  targetSpeed = 0;
	  int accelOffset = 0;
	  int deadReckoning = ideal_angle;
	  htim1.Instance->CCR4 = wheelStraight + (total_angle - deadReckoning) * 4;
	  //One revolution is 1559
	  targetRotations = cmDist * 76.4215686275 ;
	  int stopMarginDist = stopMargin > 80 + (cmDist) / 4 ? 80 + (cmDist) / 4: stopMargin;
	  __HAL_TIM_SET_COUNTER(&htim2, 0xff00);
	  __HAL_TIM_SET_COUNTER(&htim3, 0Xff00);
	  int holderA;
	  int holderB;
	  osDelay(100);

	  while(AChange < targetRotations - stopMarginDist || BChange < targetRotations - stopMarginDist){
		  if(Distance < 6){break;}

		  htim1.Instance->CCR4 = wheelStraight + (total_angle - deadReckoning) * 4;
		  holderA = __HAL_TIM_GET_COUNTER(&htim2);
		  holderB = __HAL_TIM_GET_COUNTER(&htim3);
		  AChange += abs(0xff00 - holderA);
		  BChange += abs(holderB - 0xff00);
		  __HAL_TIM_SET_COUNTER(&htim2, 0xff00);
		  __HAL_TIM_SET_COUNTER(&htim3, 0Xff00);

		  if(AChange < targetRotations - breakDelay){
			  pwmValA = accelOffset;
		  }
		  else if(AChange < targetRotations){
			  pwmValA = pwmValA <= minPWMval ? minPWMval : pwmValA - pwmValA / 4;
		  }
		  else{
			  pwmValA = 0;
		  }

		  if(BChange < targetRotations - breakDelay){
			  pwmValB = accelOffset;
		  }
		  else if(BChange < targetRotations){
			  pwmValB = pwmValB <= minPWMval ? minPWMval : pwmValB - pwmValB / 4;
		  }
		  else{
			  pwmValB = 0;
		  }

		  goForwardA(pwmValA);
		  goForwardB(pwmValB);
		  osDelay(40);
		  accelOffset += accelOffset > maxPWMval ? 0 : accelRate;
	  }
	  goBackA(pwmValA);
	  goBackB(pwmValB);
	  osDelay(50);
	  goBackA(0);
	  goBackB(0);
}

void goBackDist(int cmDist){
	  uint16_t pwmValA = 2000;
	  uint16_t pwmValB = 2000;
	  int AChange = 0;
	  int BChange = 0;
	  targetSpeed = 0;
	  int accelOffset = 0;
	  int deadReckoning = ideal_angle;
	  //One revolution is 1559
	  targetRotations = cmDist * 76.4215686275 ;
	  __HAL_TIM_SET_COUNTER(&htim2, 0xff00);
	  __HAL_TIM_SET_COUNTER(&htim3, 0xff00);
	  htim1.Instance->CCR4 = wheelStraight - (total_angle - deadReckoning) * 3;
	  int stopMarginDist = stopMargin > 80 + (cmDist) / 4 ? 80 + (cmDist) / 4: stopMargin;
	  osDelay(250);
	  while(AChange < targetRotations - stopMarginDist || BChange < targetRotations - stopMarginDist){
		  htim1.Instance->CCR4 = wheelStraight - (total_angle - deadReckoning) * 3;
		  AChange += __HAL_TIM_GET_COUNTER(&htim2) - 0xff00;
		  BChange += 0xff00 - __HAL_TIM_GET_COUNTER(&htim3);
		  __HAL_TIM_SET_COUNTER(&htim2, 0xff00);
		  __HAL_TIM_SET_COUNTER(&htim3, 0xff00);

		  if(AChange < targetRotations - breakDelay){
			  pwmValA = accelOffset;
		  }
		  else if(AChange < targetRotations){
			  pwmValA = pwmValA <= minPWMval ? minPWMval : pwmValA - pwmValA / 4;
		  }
		  else{
			  pwmValA = 0;
		  }

		  if(BChange < targetRotations - breakDelay){
			  pwmValB = accelOffset;
		  }
		  else if(BChange < targetRotations){
			  pwmValB = pwmValB <= minPWMval ? minPWMval : pwmValB - pwmValB / 4;
		  }
		  else{
			  pwmValB = 0;
		  }
		  goBackA(pwmValA);
		  goBackB(pwmValB);
		  accelOffset += accelOffset > maxPWMval ? 0 : accelRate;
		  osDelay(60);
	  }
	  goForwardA(pwmValA);
	  goForwardB(pwmValB);
	  osDelay(100);
	  goForwardA(0);
	  goForwardB(0);
}

void adjustDist(int cmDist){
	cmDist += 5;
	goForwardA(0);
	goForwardB(0);
	osDelay(200);

	if(cmDist > 150 || Distance > cmDist + 180 || Distance < 5){

		return;
	}
	else{
		if(Distance > cmDist) goForwardDist(Distance - cmDist);
		if(cmDist > Distance) goBackDist(cmDist - Distance);

	}

	goForwardA(0);
	goForwardB(0);
}


void turnRight90(int angle){
	  uint8_t hello[20];
	  ideal_angle -= angle;
		htim1.Instance->CCR4 = wheelStraight + rightTurnOffset;
		osDelay(200);
	while(total_angle >= ideal_angle + degreeOffset){
		goForwardA(turnPWMval);
		goForwardB(turnPWMval);
		osDelay(50);

	}
	  goBackA(turnPWMval);
	  goBackB(turnPWMval);
	  htim1.Instance->CCR4 = wheelStraight;
	  osDelay(50);
	goForwardA(0);
	goForwardB(0);
}

void turnLeft90(int angle){
	//3601 is encoder ticks per turn
	ideal_angle += angle;
	angle = angle + total_angle;
	htim1.Instance->CCR4 = wheelStraight - (leftTurnOffset);
	osDelay(200);
	while(total_angle <= ideal_angle - degreeOffset){
		goForwardA(turnPWMval);
		goForwardB(turnPWMval);
		osDelay(40);

	}
	goBackA(turnPWMval);
	goBackB(turnPWMval);
	htim1.Instance->CCR4 = wheelStraight;
	osDelay(50);
	goForwardA(0);
	goForwardB(0);
}

void turnLeft90Back(int angle){
	ideal_angle -= angle;
	angle = total_angle - angle;
	htim1.Instance->CCR4 = wheelStraight - leftBackTurnOffset;
	osDelay(300);
	while(total_angle > ideal_angle + degreeOffsetBack){
		goBackA(turnPWMvalBack);
		goBackB(turnPWMvalBack);
		osDelay(50);

	}
	goForwardA(turnPWMvalBack);
	goForwardB(turnPWMvalBack);
	htim1.Instance->CCR4 = wheelStraight;
	osDelay(150);
	goForwardA(0);
	goForwardB(0);
}

void turnRight90Back(int angle){
	ideal_angle += angle;
	angle = angle + total_angle;
	htim1.Instance->CCR4 = wheelStraight + rightBackTurnOffset;
	osDelay(300);
	while(total_angle < ideal_angle - degreeOffsetBack){
		goBackA(turnPWMval);
		goBackB(turnPWMval);
		osDelay(50);

	}
	goForwardA(turnPWMval);
	goForwardB(turnPWMval);
	htim1.Instance->CCR4 = wheelStraight;

	osDelay(150);
	goForwardA(0);
	goForwardB(0);
}

void leftAround(){
	turnLeft90(50);
	turnRight90(50);
	htim1.Instance->CCR4 = wheelStraight;
	while(IRRight < 35){
		 htim1.Instance->CCR4 = wheelStraight + (total_angle - ideal_angle) * 3;
		goForwardA(minPWMval);
		goForwardB(minPWMval);
		osDelay(80);
	}

	goBackA(minPWMval);
	goBackB(minPWMval);
	osDelay(150);
	goForwardA(0);
	goForwardB(0);
	osDelay(1000);
	capobDist2 = 1;
	obDist2Pure = Distance;
	obDist2 = Distance + obDist1 + 15;
	goForwardDist(5);
	if(obDist2Pure < 65){
		turnRight90(20);
		turnLeft90(20);
	}
	else{
		turnRight90(50);
		turnLeft90(50);
	}
}

void leftAround2(){
	int prevIR;
	int degrees;
	//Turn left
	turnLeft90(90);
	if(obDist2Pure < 65){
		goBackDist(30);
	}
	else{
		goBackDist(20);
	}
	goForwardA(minPWMval);
	goForwardB(minPWMval);

	//Find edge of 2nd ob
	while(IRRight < 30){ osDelay(100);}
	//Travel length of 2nd ob
	turnRight90(180);
	goForwardA(minPWMval);
	goForwardB(minPWMval);
	while(IRRight > 55){
	 htim1.Instance->CCR4 = wheelStraight + (total_angle - ideal_angle) * 3;
	 osDelay(80);
	}
	scanDistRight();

	//Turn back
	turnRight90(90);

	//New
	goForwardDist(obDist2Pure + 10);
	turnRight90(90);
	if(obWidth2 > 60){
		goForwardDist(obWidth2 - 60);
	}
	else{
		goBackDist(60 - obWidth2);
	}
	turnLeft90(90);
	while(Distance > 20){
		htim1.Instance->CCR4 = wheelStraight + (total_angle - ideal_angle) * 3;
		goForwardA(minPWMval);
		goForwardB(minPWMval);
		osDelay(60);
	}
	goBackA(minPWMval);
	goBackB(minPWMval);
	osDelay(150);
	goForwardA(0);
	goForwardB(0);
	return;
	//New end
	//goForwardDist(obDist2 - obDist1 - 10);
	//Turn to carpark
	double rat =  ((double) obWidth2) /((double) obDist2);
	degrees = atan(rat) * 57.2957795131;
	double x, y;
	x = pow(obDist2, 2);
	y = pow(obWidth2, 2);
	int distTravel = sqrt(((double) (x + y))) - 7;
	if(degrees <= 30){
		if(obWidth2 > 60){
			forwardComp = obDist2Pure;
			rat = ((double) obWidth2) / ((double) obDist1 - 5);
			degrees = atan(rat) * 57.2957795131;
			x = pow(obDist1, 2);
			distTravel = sqrt(((double) (x + y))) - 8;
			goForwardDist(forwardComp);
		}
		else{
			goForwardDist(obDist2Pure / 2);
			goForwardA(minPWMval);
			goForwardB(minPWMval);
			htim1.Instance->CCR4 = wheelStraight + (total_angle - ideal_angle) * 3;
			int maxComp = obWidth2 * 1.5 > 78 ? 74 : obWidth2 * 1.5;
			while(IRRight > maxComp){
				 htim1.Instance->CCR4 = wheelStraight + (total_angle - ideal_angle) * 3;
				 osDelay(80);
			}
			rat = ((double) (IRRight + 10)) / ((double) (obDist1 - 5));
			degrees = atan(rat) * 57.2957795131;
			x = pow(obDist1, 2);
			y = pow(IRRight + 10, 2);
			distTravel = sqrt(((double) (x + y)));
			sprintf(globalMessages, "IR%3d obD%3d", IRLeft + 10, obDist1);


		}
	}
	//sprintf(globalMessages, "D%3d DT%3d", degrees, distTravel);

	turnRight90(degrees);
	goForwardDist(distTravel);

	turnLeft90(degrees);
	while(Distance > 22 && Distance < 150){
		goForwardA(minPWMval);
		goForwardB(minPWMval);
		osDelay(50);
	}
	goBackA(minPWMval);
	goBackB(minPWMval);
	osDelay(60);
	goForwardA(0);
	goForwardB(0);
}


void rightAround(){
	turnRight90(50);
	turnLeft90(50);
	htim1.Instance->CCR4 = wheelStraight;
	while(IRLeft < 35){
		 htim1.Instance->CCR4 = wheelStraight + (total_angle - ideal_angle) * 3;
		goForwardA(minPWMval);
		goForwardB(minPWMval);
		osDelay(80);
	}

	goBackA(minPWMval);
	goBackB(minPWMval);
	osDelay(150);
	goForwardA(0);
	goForwardB(0);

	osDelay(1000);
	obDist2 = Distance + obDist1 + 15;
	obDist2Pure = Distance;

	goForwardDist(5);
	if(obDist2Pure < 65){
		turnLeft90(20 - 2);
		turnRight90(20);
	}
	else{
		turnLeft90(50);
		turnRight90(50);
	}
}

void rightAround2(){
	int degrees = 0;
	//Turn left
	turnRight90(90);
	if(obDist2Pure < 65){
		goBackDist(30);
	}
	else{
		goBackDist(20);
	}
	goForwardA(minPWMval);
	goForwardB(minPWMval);

	//Find edge of 2nd ob
	while(IRLeft < 40){ osDelay(50);}
	//Travel length of 2nd ob
	turnLeft90(180);
	 htim1.Instance->CCR4 = wheelStraight + (total_angle - ideal_angle) * 3;
	goForwardA(minPWMval);
	goForwardB(minPWMval);
	while(IRLeft > 55){
		htim1.Instance->CCR4 = wheelStraight + (total_angle - ideal_angle) * 3;
		osDelay(80);
	}
	scanDist();
	//Turn back
	turnLeft90(90);
	goForwardDist(obDist2Pure + 10);
	turnLeft90(90);
	if(obWidth2 > 60){
		goForwardDist(obWidth2 - 60);
	}
	else{
		goBackDist(60 - obWidth2);
	}
	turnRight90(90);
	while(Distance > 20){
		htim1.Instance->CCR4 = wheelStraight + (total_angle - ideal_angle) * 3;
		goForwardA(minPWMval);
		goForwardB(minPWMval);
		osDelay(60);
	}
	goBackA(minPWMval);
	goBackB(minPWMval);
	osDelay(150);
	goForwardA(0);
	goForwardB(0);
	return;
	//goForwardDist(obDist2 - obDist1);

	//goForwardDist(obDist2 - obDist1 - 10);
	//Turn to carpark
	double rat =  ((double) obWidth2) /((double) obDist2);
	degrees = atan(rat) * 57.2957795131;
	double x, y;
	x = pow(obDist2, 2);
	y = pow(obWidth2, 2);
	int distTravel = sqrt(((double) (x + y))) - 7;
	if(degrees <= 65){
		if(obWidth2 > 65){
			forwardComp = obDist2Pure;
			rat = ((double) obWidth2) / ((double) obDist1);
			degrees = atan(rat) * 57.2957795131;
			x = pow(obDist1, 2);
			distTravel = sqrt(((double) (x + y))) - 8;
			goForwardDist(forwardComp);
		}
		else{
			goForwardA(minPWMval);
			goForwardB(minPWMval);
			htim1.Instance->CCR4 = wheelStraight + (total_angle - ideal_angle) * 3;
			goForwardDist(obDist2Pure / 2);
			goForwardA(minPWMval);
			goForwardB(minPWMval);
			int maxComp = obWidth2 * 1.5 > 78 ? 74 : obWidth2 * 1.5;
			while(IRRight > maxComp){
				htim1.Instance->CCR4 = wheelStraight + (total_angle - ideal_angle) * 3;
				 osDelay(80);
			}

			rat = ((double) IRLeft + 10) / ((double) obDist1);
			degrees = atan(rat) * 57.2957795131;
			x = pow(obDist1, 2);
			y = pow(IRLeft + 10, 2);
			distTravel = sqrt(((double) (x + y)));
			sprintf(globalMessages, "IR%3d obD%3d", IRLeft + 10, obDist1);


		}
	}


	turnLeft90(degrees);
	goForwardDist(distTravel);


	turnRight90(degrees);
	while(Distance > 22 && Distance < 150){
		goForwardA(minPWMval);
		goForwardB(minPWMval);
		osDelay(50);
	}
	goBackA(minPWMval);
	goBackB(minPWMval);
	osDelay(60);
	goForwardA(0);
	goForwardB(0);
}

void scanDist(){
	unsigned int AChange = 0;
	unsigned int BChange = 0;
	goForwardA(minPWMval);
	goForwardB(minPWMval);
	 __HAL_TIM_SET_COUNTER(&htim2, 0xff00);
	 __HAL_TIM_SET_COUNTER(&htim3, 0Xff00);
	 int deadReckoning = ideal_angle;
	while(IRLeft < 50){

		 htim1.Instance->CCR4 = wheelStraight + (total_angle - deadReckoning) * 2;
		 AChange += abs(0xff00 - __HAL_TIM_GET_COUNTER(&htim2));
		 BChange += abs(__HAL_TIM_GET_COUNTER(&htim3) - 0Xff00);
		 __HAL_TIM_SET_COUNTER(&htim2, 0xff00);
		 __HAL_TIM_SET_COUNTER(&htim3, 0Xff00);
		osDelay(100);
	}
	obWidth2 = (AChange) / 2 / 76.4215686275 + 20 + 13;
}

void scanDistRight(){
	unsigned int AChange = 0;
	unsigned int BChange = 0;
	goForwardA(minPWMval);
	goForwardB(minPWMval);
	 __HAL_TIM_SET_COUNTER(&htim2, 0xff00);
	 __HAL_TIM_SET_COUNTER(&htim3, 0Xff00);
	 int deadReckoning = ideal_angle;
	while(IRRight < 50){

		 htim1.Instance->CCR4 = wheelStraight + (total_angle - deadReckoning) * 2;
		 AChange += abs(0xff00 - __HAL_TIM_GET_COUNTER(&htim2));
		 BChange += abs(__HAL_TIM_GET_COUNTER(&htim3) - 0Xff00);
		 __HAL_TIM_SET_COUNTER(&htim2, 0xff00);
		 __HAL_TIM_SET_COUNTER(&htim3, 0Xff00);
		osDelay(100);
	}
	obWidth2 = (AChange) / 2 / 76.4215686275 + 20 + 13;
}

void onSpotturnRight90(int angle){
	angle = total_angle - angle;
	while(total_angle > angle){
		htim1.Instance->CCR4 = wheelStraight + rightTurnOffset;
		goForwardA(1500);
		goForwardB(1500);
		osDelay(700);
		if(total_angle <= angle){break;}
		goBackA(1500);
		goBackB(1500);
		htim1.Instance->CCR4 = wheelStraight - leftTurnOffset;
		osDelay(700);

	}
	goForwardA(0);
	goForwardB(0);
	htim1.Instance->CCR4 = wheelStraight;
	osDelay(400);
}

void onSpotturnLeft90(int angle){
	angle = angle + total_angle;
	while(total_angle < angle){
		htim1.Instance->CCR4 = wheelStraight - leftTurnOffset;
		goForwardA(1300);
		goForwardB(1300);
		osDelay(600);
		if (total_angle >= angle){break;}
		goBackA(1300);
		goBackB(1300);
		htim1.Instance->CCR4 = wheelStraight + leftTurnOffset;
		osDelay(600);

	}
	goBackA(turnPWMval);
	goBackB(turnPWMval);
	osDelay(100);
	goForwardA(0);
	goForwardB(0);
	htim1.Instance->CCR4 = wheelStraight;
	osDelay(400);
}

void readByte(uint8_t addr, uint8_t* data){
	gyroBuffer[0] = addr;
	HAL_I2C_Master_Transmit(&hi2c1, ICMAddress<<1, gyroBuffer, 1, 10);
	HAL_I2C_Master_Receive(&hi2c1, ICMAddress<<1, data, 2, 20);
}

void writeByte(uint8_t addr, uint8_t data){
	gyroBuffer[0] = addr;
	gyroBuffer[1] = data;
	HAL_I2C_Master_Transmit(&hi2c1, ICMAddress << 1, gyroBuffer, 2, 20);
}

void gyroInit(){
	writeByte(0x06, 0x00);
	osDelay(10);
	writeByte(0x03, 0x80);
	osDelay(10);
	writeByte(0x07, 0x07);
	osDelay(10);
	writeByte(0x06, 0x01);
	osDelay(10);
	writeByte(0x7F, 0x20);
	osDelay(10);
	writeByte(0x01, 0x2F);
	osDelay(10);
	writeByte(0x0, 0x00);
	osDelay(10);
	writeByte(0x7F, 0x00);
	osDelay(10);
	writeByte(0x07, 0x00);
	osDelay(10);
}

void HAL_TIM_IC_CaptureCallback(TIM_HandleTypeDef *htim)
{
	if(htim->Channel == HAL_TIM_ACTIVE_CHANNEL_1) //if the interrupt source is channel 1
	{
		if(Is_First_Captured==0)
		{
			IC_Val1 = HAL_TIM_ReadCapturedValue(htim, TIM_CHANNEL_1); //read the first value
			Is_First_Captured = 1;//set first capture as true
			//Now change the polarity to falling edge
			__HAL_TIM_SET_CAPTUREPOLARITY(htim, TIM_CHANNEL_1, TIM_INPUTCHANNELPOLARITY_FALLING);
		}
		else if (Is_First_Captured==1)
		{
			IC_Val2 = HAL_TIM_ReadCapturedValue(htim, TIM_CHANNEL_1); //read second value
			__HAL_TIM_SET_COUNTER(htim, 0); //reset counter

			if(IC_Val2>IC_Val1)
			{
				Difference = IC_Val2 - IC_Val1;
			}
			else if(IC_Val1>IC_Val2)
			{
				Difference = (0xffff - IC_Val1) + IC_Val2;
			}

			Distance = Difference * .034/2;
			Is_First_Captured = 0; //set it back to false

			// set polarity to rising edge
			__HAL_TIM_SET_CAPTUREPOLARITY(htim,TIM_CHANNEL_1,TIM_INPUTCHANNELPOLARITY_RISING);
			__HAL_TIM_DISABLE_IT(&htim4, TIM_IT_CC1);
		}
	}
}

void HCSR04_Read (void)
{
	HAL_GPIO_WritePin(TRIG_PORT, TRIG_PIN, GPIO_PIN_SET);  // pull the TRIG pin HIGH
	delay(10);  // wait for 10 us
	HAL_GPIO_WritePin(TRIG_PORT, TRIG_PIN, GPIO_PIN_RESET);  // pull the TRIG pin low
	  uint8_t hello[20];


	__HAL_TIM_ENABLE_IT(&htim4, TIM_IT_CC1);

}

void delay(uint16_t time){

	 __HAL_TIM_SET_COUNTER(&htim4, 0);
	 //int test = 0;
	 while(__HAL_TIM_GET_COUNTER(&htim4) < time){
	 }

}
/* USER CODE END 0 */

/**
  * @brief  The application entry point.
  * @retval int
  */
int main(void)
{
  /* USER CODE BEGIN 1 */
  /* USER CODE END 1 */

  /* MCU Configuration--------------------------------------------------------*/

  /* Reset of all peripherals, Initializes the Flash interface and the Systick. */
  HAL_Init();

  /* USER CODE BEGIN Init */

  /* USER CODE END Init */

  /* Configure the system clock */
  SystemClock_Config();

  /* USER CODE BEGIN SysInit */

  /* USER CODE END SysInit */

  /* Initialize all configured peripherals */
  MX_GPIO_Init();
  MX_TIM8_Init();
  MX_TIM2_Init();
  MX_TIM1_Init();
  MX_USART3_UART_Init();
  MX_TIM3_Init();
  MX_TIM4_Init();
  MX_I2C1_Init();
  MX_ADC1_Init();
  MX_ADC2_Init();

  /* Initialize interrupts */
  MX_NVIC_Init();
  /* USER CODE BEGIN 2 */

  /* USER CODE END 2 */

  /* Init scheduler */
  osKernelInitialize();

  /* USER CODE BEGIN RTOS_MUTEX */
  /* add mutexes, ... */
  /* USER CODE END RTOS_MUTEX */

  /* USER CODE BEGIN RTOS_SEMAPHORES */
  /* add semaphores, ... */
  /* USER CODE END RTOS_SEMAPHORES */

  /* USER CODE BEGIN RTOS_TIMERS */
  /* start timers, add new ones, ... */
  /* USER CODE END RTOS_TIMERS */

  /* USER CODE BEGIN RTOS_QUEUES */
  /* add queues, ... */
  /* USER CODE END RTOS_QUEUES */

  /* Create the thread(s) */
  /* creation of UARTask */
  UARTaskHandle = osThreadNew(UartTask, NULL, &UARTask_attributes);

  /* creation of showOled */
  showOledHandle = osThreadNew(show, NULL, &showOled_attributes);

  /* creation of Ultrasonic */
  UltrasonicHandle = osThreadNew(ultrasonicTask, NULL, &Ultrasonic_attributes);

  /* creation of gyro */
  gyroHandle = osThreadNew(GyroTask, NULL, &gyro_attributes);

  /* creation of IRtask */
  //IRtaskHandle = osThreadNew(IRTask, NULL, &IRtask_attributes);

  /* USER CODE BEGIN RTOS_THREADS */
  /* add threads, ... */
  /* USER CODE END RTOS_THREADS */

  /* USER CODE BEGIN RTOS_EVENTS */
  /* add events, ... */
  /* USER CODE END RTOS_EVENTS */

  /* Start scheduler */
  osKernelStart();

  /* We should never get here as control is now taken by the scheduler */
  /* Infinite loop */
  /* USER CODE BEGIN WHILE */
  while (1)
  {
    /* USER CODE END WHILE */

    /* USER CODE BEGIN 3 */
  }
  /* USER CODE END 3 */
}

/**
  * @brief System Clock Configuration
  * @retval None
  */
void SystemClock_Config(void)
{
  RCC_OscInitTypeDef RCC_OscInitStruct = {0};
  RCC_ClkInitTypeDef RCC_ClkInitStruct = {0};

  /** Configure the main internal regulator output voltage
  */
  __HAL_RCC_PWR_CLK_ENABLE();
  __HAL_PWR_VOLTAGESCALING_CONFIG(PWR_REGULATOR_VOLTAGE_SCALE1);

  /** Initializes the RCC Oscillators according to the specified parameters
  * in the RCC_OscInitTypeDef structure.
  */
  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSI;
  RCC_OscInitStruct.HSIState = RCC_HSI_ON;
  RCC_OscInitStruct.HSICalibrationValue = RCC_HSICALIBRATION_DEFAULT;
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_NONE;
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)
  {
    Error_Handler();
  }

  /** Initializes the CPU, AHB and APB buses clocks
  */
  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK|RCC_CLOCKTYPE_SYSCLK
                              |RCC_CLOCKTYPE_PCLK1|RCC_CLOCKTYPE_PCLK2;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_HSI;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV1;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_0) != HAL_OK)
  {
    Error_Handler();
  }
}

/**
  * @brief NVIC Configuration.
  * @retval None
  */
static void MX_NVIC_Init(void)
{
  /* TIM4_IRQn interrupt configuration */
  HAL_NVIC_SetPriority(TIM4_IRQn, 5, 0);
  HAL_NVIC_EnableIRQ(TIM4_IRQn);
}

/**
  * @brief ADC1 Initialization Function
  * @param None
  * @retval None
  */
static void MX_ADC1_Init(void)
{

  /* USER CODE BEGIN ADC1_Init 0 */

  /* USER CODE END ADC1_Init 0 */

  ADC_ChannelConfTypeDef sConfig = {0};

  /* USER CODE BEGIN ADC1_Init 1 */

  /* USER CODE END ADC1_Init 1 */

  /** Configure the global features of the ADC (Clock, Resolution, Data Alignment and number of conversion)
  */
  hadc1.Instance = ADC1;
  hadc1.Init.ClockPrescaler = ADC_CLOCK_SYNC_PCLK_DIV2;
  hadc1.Init.Resolution = ADC_RESOLUTION_12B;
  hadc1.Init.ScanConvMode = DISABLE;
  hadc1.Init.ContinuousConvMode = DISABLE;
  hadc1.Init.DiscontinuousConvMode = DISABLE;
  hadc1.Init.ExternalTrigConvEdge = ADC_EXTERNALTRIGCONVEDGE_NONE;
  hadc1.Init.ExternalTrigConv = ADC_SOFTWARE_START;
  hadc1.Init.DataAlign = ADC_DATAALIGN_RIGHT;
  hadc1.Init.NbrOfConversion = 1;
  hadc1.Init.DMAContinuousRequests = DISABLE;
  hadc1.Init.EOCSelection = ADC_EOC_SINGLE_CONV;
  if (HAL_ADC_Init(&hadc1) != HAL_OK)
  {
    Error_Handler();
  }

  /** Configure for the selected ADC regular channel its corresponding rank in the sequencer and its sample time.
  */
  sConfig.Channel = ADC_CHANNEL_0;
  sConfig.Rank = 1;
  sConfig.SamplingTime = ADC_SAMPLETIME_3CYCLES;
  if (HAL_ADC_ConfigChannel(&hadc1, &sConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN ADC1_Init 2 */

  /* USER CODE END ADC1_Init 2 */

}

/**
  * @brief ADC2 Initialization Function
  * @param None
  * @retval None
  */
static void MX_ADC2_Init(void)
{

  /* USER CODE BEGIN ADC2_Init 0 */

  /* USER CODE END ADC2_Init 0 */

  ADC_ChannelConfTypeDef sConfig = {0};

  /* USER CODE BEGIN ADC2_Init 1 */

  /* USER CODE END ADC2_Init 1 */

  /** Configure the global features of the ADC (Clock, Resolution, Data Alignment and number of conversion)
  */
  hadc2.Instance = ADC2;
  hadc2.Init.ClockPrescaler = ADC_CLOCK_SYNC_PCLK_DIV2;
  hadc2.Init.Resolution = ADC_RESOLUTION_12B;
  hadc2.Init.ScanConvMode = DISABLE;
  hadc2.Init.ContinuousConvMode = DISABLE;
  hadc2.Init.DiscontinuousConvMode = DISABLE;
  hadc2.Init.ExternalTrigConvEdge = ADC_EXTERNALTRIGCONVEDGE_NONE;
  hadc2.Init.ExternalTrigConv = ADC_SOFTWARE_START;
  hadc2.Init.DataAlign = ADC_DATAALIGN_RIGHT;
  hadc2.Init.NbrOfConversion = 1;
  hadc2.Init.DMAContinuousRequests = DISABLE;
  hadc2.Init.EOCSelection = ADC_EOC_SINGLE_CONV;
  if (HAL_ADC_Init(&hadc2) != HAL_OK)
  {
    Error_Handler();
  }

  /** Configure for the selected ADC regular channel its corresponding rank in the sequencer and its sample time.
  */
  sConfig.Channel = ADC_CHANNEL_1;
  sConfig.Rank = 1;
  sConfig.SamplingTime = ADC_SAMPLETIME_3CYCLES;
  if (HAL_ADC_ConfigChannel(&hadc2, &sConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN ADC2_Init 2 */

  /* USER CODE END ADC2_Init 2 */

}

/**
  * @brief I2C1 Initialization Function
  * @param None
  * @retval None
  */
static void MX_I2C1_Init(void)
{

  /* USER CODE BEGIN I2C1_Init 0 */

  /* USER CODE END I2C1_Init 0 */

  /* USER CODE BEGIN I2C1_Init 1 */

  /* USER CODE END I2C1_Init 1 */
  hi2c1.Instance = I2C1;
  hi2c1.Init.ClockSpeed = 100000;
  hi2c1.Init.DutyCycle = I2C_DUTYCYCLE_2;
  hi2c1.Init.OwnAddress1 = 0;
  hi2c1.Init.AddressingMode = I2C_ADDRESSINGMODE_7BIT;
  hi2c1.Init.DualAddressMode = I2C_DUALADDRESS_DISABLE;
  hi2c1.Init.OwnAddress2 = 0;
  hi2c1.Init.GeneralCallMode = I2C_GENERALCALL_DISABLE;
  hi2c1.Init.NoStretchMode = I2C_NOSTRETCH_DISABLE;
  if (HAL_I2C_Init(&hi2c1) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN I2C1_Init 2 */

  /* USER CODE END I2C1_Init 2 */

}

/**
  * @brief TIM1 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM1_Init(void)
{

  /* USER CODE BEGIN TIM1_Init 0 */

  /* USER CODE END TIM1_Init 0 */

  TIM_ClockConfigTypeDef sClockSourceConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};
  TIM_OC_InitTypeDef sConfigOC = {0};
  TIM_BreakDeadTimeConfigTypeDef sBreakDeadTimeConfig = {0};

  /* USER CODE BEGIN TIM1_Init 1 */

  /* USER CODE END TIM1_Init 1 */
  htim1.Instance = TIM1;
  htim1.Init.Prescaler = 160;
  htim1.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim1.Init.Period = 1000;
  htim1.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim1.Init.RepetitionCounter = 0;
  htim1.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_ENABLE;
  if (HAL_TIM_Base_Init(&htim1) != HAL_OK)
  {
    Error_Handler();
  }
  sClockSourceConfig.ClockSource = TIM_CLOCKSOURCE_INTERNAL;
  if (HAL_TIM_ConfigClockSource(&htim1, &sClockSourceConfig) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_Init(&htim1) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim1, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sConfigOC.OCMode = TIM_OCMODE_PWM1;
  sConfigOC.Pulse = 0;
  sConfigOC.OCPolarity = TIM_OCPOLARITY_HIGH;
  sConfigOC.OCFastMode = TIM_OCFAST_DISABLE;
  sConfigOC.OCIdleState = TIM_OCIDLESTATE_RESET;
  sConfigOC.OCNIdleState = TIM_OCNIDLESTATE_RESET;
  if (HAL_TIM_PWM_ConfigChannel(&htim1, &sConfigOC, TIM_CHANNEL_4) != HAL_OK)
  {
    Error_Handler();
  }
  sBreakDeadTimeConfig.OffStateRunMode = TIM_OSSR_DISABLE;
  sBreakDeadTimeConfig.OffStateIDLEMode = TIM_OSSI_DISABLE;
  sBreakDeadTimeConfig.LockLevel = TIM_LOCKLEVEL_OFF;
  sBreakDeadTimeConfig.DeadTime = 0;
  sBreakDeadTimeConfig.BreakState = TIM_BREAK_DISABLE;
  sBreakDeadTimeConfig.BreakPolarity = TIM_BREAKPOLARITY_HIGH;
  sBreakDeadTimeConfig.AutomaticOutput = TIM_AUTOMATICOUTPUT_DISABLE;
  if (HAL_TIMEx_ConfigBreakDeadTime(&htim1, &sBreakDeadTimeConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM1_Init 2 */

  /* USER CODE END TIM1_Init 2 */
  HAL_TIM_MspPostInit(&htim1);

}

/**
  * @brief TIM2 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM2_Init(void)
{

  /* USER CODE BEGIN TIM2_Init 0 */

  /* USER CODE END TIM2_Init 0 */

  TIM_Encoder_InitTypeDef sConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};

  /* USER CODE BEGIN TIM2_Init 1 */

  /* USER CODE END TIM2_Init 1 */
  htim2.Instance = TIM2;
  htim2.Init.Prescaler = 0;
  htim2.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim2.Init.Period = 65535;
  htim2.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim2.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  sConfig.EncoderMode = TIM_ENCODERMODE_TI12;
  sConfig.IC1Polarity = TIM_ICPOLARITY_RISING;
  sConfig.IC1Selection = TIM_ICSELECTION_DIRECTTI;
  sConfig.IC1Prescaler = TIM_ICPSC_DIV1;
  sConfig.IC1Filter = 10;
  sConfig.IC2Polarity = TIM_ICPOLARITY_RISING;
  sConfig.IC2Selection = TIM_ICSELECTION_DIRECTTI;
  sConfig.IC2Prescaler = TIM_ICPSC_DIV1;
  sConfig.IC2Filter = 10;
  if (HAL_TIM_Encoder_Init(&htim2, &sConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim2, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM2_Init 2 */

  /* USER CODE END TIM2_Init 2 */

}

/**
  * @brief TIM3 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM3_Init(void)
{

  /* USER CODE BEGIN TIM3_Init 0 */

  /* USER CODE END TIM3_Init 0 */

  TIM_Encoder_InitTypeDef sConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};

  /* USER CODE BEGIN TIM3_Init 1 */

  /* USER CODE END TIM3_Init 1 */
  htim3.Instance = TIM3;
  htim3.Init.Prescaler = 0;
  htim3.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim3.Init.Period = 65535;
  htim3.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim3.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  sConfig.EncoderMode = TIM_ENCODERMODE_TI12;
  sConfig.IC1Polarity = TIM_ICPOLARITY_RISING;
  sConfig.IC1Selection = TIM_ICSELECTION_DIRECTTI;
  sConfig.IC1Prescaler = TIM_ICPSC_DIV1;
  sConfig.IC1Filter = 10;
  sConfig.IC2Polarity = TIM_ICPOLARITY_RISING;
  sConfig.IC2Selection = TIM_ICSELECTION_DIRECTTI;
  sConfig.IC2Prescaler = TIM_ICPSC_DIV1;
  sConfig.IC2Filter = 10;
  if (HAL_TIM_Encoder_Init(&htim3, &sConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim3, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM3_Init 2 */

  /* USER CODE END TIM3_Init 2 */

}

/**
  * @brief TIM4 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM4_Init(void)
{

  /* USER CODE BEGIN TIM4_Init 0 */

  /* USER CODE END TIM4_Init 0 */

  TIM_ClockConfigTypeDef sClockSourceConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};
  TIM_IC_InitTypeDef sConfigIC = {0};

  /* USER CODE BEGIN TIM4_Init 1 */

  /* USER CODE END TIM4_Init 1 */
  htim4.Instance = TIM4;
  htim4.Init.Prescaler = 15;
  htim4.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim4.Init.Period = 0xffff - 1;
  htim4.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim4.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  if (HAL_TIM_Base_Init(&htim4) != HAL_OK)
  {
    Error_Handler();
  }
  sClockSourceConfig.ClockSource = TIM_CLOCKSOURCE_INTERNAL;
  if (HAL_TIM_ConfigClockSource(&htim4, &sClockSourceConfig) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_IC_Init(&htim4) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim4, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sConfigIC.ICPolarity = TIM_INPUTCHANNELPOLARITY_RISING;
  sConfigIC.ICSelection = TIM_ICSELECTION_DIRECTTI;
  sConfigIC.ICPrescaler = TIM_ICPSC_DIV1;
  sConfigIC.ICFilter = 0;
  if (HAL_TIM_IC_ConfigChannel(&htim4, &sConfigIC, TIM_CHANNEL_1) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM4_Init 2 */

  /* USER CODE END TIM4_Init 2 */

}

/**
  * @brief TIM8 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM8_Init(void)
{

  /* USER CODE BEGIN TIM8_Init 0 */

  /* USER CODE END TIM8_Init 0 */

  TIM_ClockConfigTypeDef sClockSourceConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};
  TIM_OC_InitTypeDef sConfigOC = {0};
  TIM_BreakDeadTimeConfigTypeDef sBreakDeadTimeConfig = {0};

  /* USER CODE BEGIN TIM8_Init 1 */

  /* USER CODE END TIM8_Init 1 */
  htim8.Instance = TIM8;
  htim8.Init.Prescaler = 0;
  htim8.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim8.Init.Period = 7199;
  htim8.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim8.Init.RepetitionCounter = 0;
  htim8.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  if (HAL_TIM_Base_Init(&htim8) != HAL_OK)
  {
    Error_Handler();
  }
  sClockSourceConfig.ClockSource = TIM_CLOCKSOURCE_INTERNAL;
  if (HAL_TIM_ConfigClockSource(&htim8, &sClockSourceConfig) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_Init(&htim8) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim8, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sConfigOC.OCMode = TIM_OCMODE_PWM1;
  sConfigOC.Pulse = 0;
  sConfigOC.OCPolarity = TIM_OCPOLARITY_HIGH;
  sConfigOC.OCNPolarity = TIM_OCNPOLARITY_HIGH;
  sConfigOC.OCFastMode = TIM_OCFAST_DISABLE;
  sConfigOC.OCIdleState = TIM_OCIDLESTATE_RESET;
  sConfigOC.OCNIdleState = TIM_OCNIDLESTATE_RESET;
  if (HAL_TIM_PWM_ConfigChannel(&htim8, &sConfigOC, TIM_CHANNEL_1) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_ConfigChannel(&htim8, &sConfigOC, TIM_CHANNEL_2) != HAL_OK)
  {
    Error_Handler();
  }
  sBreakDeadTimeConfig.OffStateRunMode = TIM_OSSR_DISABLE;
  sBreakDeadTimeConfig.OffStateIDLEMode = TIM_OSSI_DISABLE;
  sBreakDeadTimeConfig.LockLevel = TIM_LOCKLEVEL_OFF;
  sBreakDeadTimeConfig.DeadTime = 0;
  sBreakDeadTimeConfig.BreakState = TIM_BREAK_DISABLE;
  sBreakDeadTimeConfig.BreakPolarity = TIM_BREAKPOLARITY_HIGH;
  sBreakDeadTimeConfig.AutomaticOutput = TIM_AUTOMATICOUTPUT_DISABLE;
  if (HAL_TIMEx_ConfigBreakDeadTime(&htim8, &sBreakDeadTimeConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM8_Init 2 */

  /* USER CODE END TIM8_Init 2 */

}

/**
  * @brief USART3 Initialization Function
  * @param None
  * @retval None
  */
static void MX_USART3_UART_Init(void)
{

  /* USER CODE BEGIN USART3_Init 0 */

  /* USER CODE END USART3_Init 0 */

  /* USER CODE BEGIN USART3_Init 1 */

  /* USER CODE END USART3_Init 1 */
  huart3.Instance = USART3;
  huart3.Init.BaudRate = 115200;
  huart3.Init.WordLength = UART_WORDLENGTH_8B;
  huart3.Init.StopBits = UART_STOPBITS_1;
  huart3.Init.Parity = UART_PARITY_NONE;
  huart3.Init.Mode = UART_MODE_TX_RX;
  huart3.Init.HwFlowCtl = UART_HWCONTROL_NONE;
  huart3.Init.OverSampling = UART_OVERSAMPLING_16;
  if (HAL_UART_Init(&huart3) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN USART3_Init 2 */

  /* USER CODE END USART3_Init 2 */

}

/**
  * @brief GPIO Initialization Function
  * @param None
  * @retval None
  */
static void MX_GPIO_Init(void)
{
  GPIO_InitTypeDef GPIO_InitStruct = {0};
/* USER CODE BEGIN MX_GPIO_Init_1 */
/* USER CODE END MX_GPIO_Init_1 */

  /* GPIO Ports Clock Enable */
  __HAL_RCC_GPIOE_CLK_ENABLE();
  __HAL_RCC_GPIOH_CLK_ENABLE();
  __HAL_RCC_GPIOA_CLK_ENABLE();
  __HAL_RCC_GPIOB_CLK_ENABLE();
  __HAL_RCC_GPIOD_CLK_ENABLE();
  __HAL_RCC_GPIOC_CLK_ENABLE();

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOE, OLED_SCL_Pin|OLED_SDA_Pin|OLED_RESET_Pin|OLED_DC_Pin
                          |LED3_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOA, AIN2_Pin|AIN1_Pin|BIN1_Pin|BIN2_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(Buzzer_GPIO_Port, Buzzer_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOD, GPIO_PIN_9, GPIO_PIN_RESET);

  /*Configure GPIO pins : OLED_SCL_Pin OLED_SDA_Pin OLED_RESET_Pin OLED_DC_Pin
                           LED3_Pin */
  GPIO_InitStruct.Pin = OLED_SCL_Pin|OLED_SDA_Pin|OLED_RESET_Pin|OLED_DC_Pin
                          |LED3_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOE, &GPIO_InitStruct);

  /*Configure GPIO pins : AIN2_Pin AIN1_Pin BIN1_Pin BIN2_Pin */
  GPIO_InitStruct.Pin = AIN2_Pin|AIN1_Pin|BIN1_Pin|BIN2_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOA, &GPIO_InitStruct);

  /*Configure GPIO pin : Buzzer_Pin */
  GPIO_InitStruct.Pin = Buzzer_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(Buzzer_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pin : PD9 */
  GPIO_InitStruct.Pin = GPIO_PIN_9;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOD, &GPIO_InitStruct);

/* USER CODE BEGIN MX_GPIO_Init_2 */
/* USER CODE END MX_GPIO_Init_2 */
}

/* USER CODE BEGIN 4 */
void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart){
	//Prevent unused args compliation warning
	UNUSED(huart);

	HAL_UART_Receive_IT(&huart3, (uint8_t *) aRxBuffer, 5);

	update = 1;
}
/* USER CODE END 4 */

/* USER CODE BEGIN Header_UartTask */
/**
  * @brief  Function implementing the UARTask thread.
  * @param  argument: Not used
  * @retval None
  */
/* USER CODE END Header_UartTask */
void UartTask(void *argument)
{
  /* USER CODE BEGIN 5 */
  /* Infinite loop */
  HAL_UART_Receive_IT(&huart3, (uint8_t *) aRxBuffer, 5);
  uint8_t buff[20];
  motorsInit();
  uint8_t resBuffer[20];

  aRxBuffer[0] = 'F';
  aRxBuffer[1] = 'W';
  aRxBuffer[2] = '0';
  aRxBuffer[3] = '2';
  aRxBuffer[4] = '0';
  //HAL_ADC_Start();

  while(notdone){ osDelay(100); };
  htim1.Instance->CCR4 = 150;


  obDist1 = Distance;
  /*
  osDelay(4000);
  int deadReckoning = ideal_angle;
  while(Distance > 50){
	  htim1.Instance->CCR4 = wheelStraight + (total_angle - deadReckoning) * 2;
	  goForwardA(minPWMval);
	  goForwardB(minPWMval);
	  osDelay(100);
  }
  adjustDist(30);
  leftAround();
  if(obDist2Pure < 70){
	  goBackDist(20);
  }
  adjustDist(30);

  leftAround2();*/
  for(;;)
  {
	if(update){
				if(aRxBuffer[0] == 'M' && aRxBuffer[1] == '1'){
				  int deadReckoning = ideal_angle;

				  while(Distance > 50){
					  htim1.Instance->CCR4 = wheelStraight + (total_angle - deadReckoning) * 2;
					  goForwardA(minPWMval);
					  goForwardB(minPWMval);
					  osDelay(100);
				  }
				  adjustDist(30);
				resBuffer[0] = 'A'; resBuffer[1] = 'C'; resBuffer[2] = 'K'; resBuffer[3] = '\\'; resBuffer[4] = 'n';
				HAL_UART_Transmit(&huart3, resBuffer, 5, 0xFFFF);
				update = 0;
				}

				else if(aRxBuffer[0] == 'M' && aRxBuffer[1] == '2'){
					if(aRxBuffer[4] == '0'){
						leftAround();
					}
					else{
						rightAround();
					}

				  if(obDist2Pure < 70){
					  if(Distance > 150) goBackDist(5);
					adjustDist(25);}
				  else{adjustDist(30);}
					  /*
					  while(Distance > 29){
						  htim1.Instance->CCR4 = wheelStraight + (total_angle - ideal_angle) * 2;
						  goForwardA(minPWMval);
						  goForwardB(minPWMval);
						  osDelay(100);
					  }
					  goBackA(minPWMval);
					  goBackB(minPWMval);*/
					  resBuffer[0] = 'A'; resBuffer[1] = 'C'; resBuffer[2] = 'K'; resBuffer[3] = '\\'; resBuffer[4] = 'n';
					HAL_UART_Transmit(&huart3, resBuffer, 5, 0xFFFF);
					update = 0;
					goForwardA(0);
					goForwardB(0);
				}

				else if(aRxBuffer[0] == 'M' && aRxBuffer[1] == '3'){
					if(aRxBuffer[4] == '0'){
						leftAround2();
					}
					else{
						rightAround2();
					}
					resBuffer[0] = 'A'; resBuffer[1] = 'C'; resBuffer[2] = 'K'; resBuffer[3] = '\\'; resBuffer[4] = 'n';
					HAL_UART_Transmit(&huart3, resBuffer, 5, 0xFFFF);
					update = 0;
				}
				else if(aRxBuffer[0] == 'F' && aRxBuffer[1] == 'W'){
					goForwardDist((aRxBuffer[2] - '0') * 100 + (aRxBuffer[3] - '0') * 10 + (aRxBuffer[4] - '0') * 1);
					resBuffer[0] = 'A'; resBuffer[1] = 'C'; resBuffer[2] = 'K'; resBuffer[3] = '\\'; resBuffer[4] = 'n';
					HAL_UART_Transmit(&huart3, resBuffer, 5, 0xFFFF);
					update = 0;
				}
				else if(aRxBuffer[0] == 'T' && aRxBuffer[1] == 'R'){
					turnRight90( (aRxBuffer[2] - '0') * 100 + (aRxBuffer[3] - '0') * 10 + (aRxBuffer[4] - '0') );
					resBuffer[0] = 'A'; resBuffer[1] = 'C'; resBuffer[2] = 'K'; resBuffer[3] = '\\'; resBuffer[4] = 'n';
					HAL_UART_Transmit(&huart3, resBuffer, 5, 0xFFFF);
					update = 0;
				}
				else if(aRxBuffer[0] == 'T' && aRxBuffer[1] == 'L'){
					turnLeft90( (aRxBuffer[2] - '0') * 100 + (aRxBuffer[3] - '0') * 10 + (aRxBuffer[4] - '0') );
					resBuffer[0] = 'A'; resBuffer[1] = 'C'; resBuffer[2] = 'K'; resBuffer[3] = '\\'; resBuffer[4] = 'n';
					HAL_UART_Transmit(&huart3, resBuffer, 5, 0xFFFF);
					update = 0;
				}

				else if(aRxBuffer[0] == 'F' && aRxBuffer[1] == 'R'){
					turnRight90( 90 );
					resBuffer[0] = 'A'; resBuffer[1] = 'C'; resBuffer[2] = 'K'; resBuffer[3] = '\\'; resBuffer[4] = 'n';
					HAL_UART_Transmit(&huart3, resBuffer, 5, 0xFFFF);
					update = 0;
				}

				else if(aRxBuffer[0] == 'F' && aRxBuffer[1] == 'L'){
					turnLeft90( 90 );
					resBuffer[0] = 'A'; resBuffer[1] = 'C'; resBuffer[2] = 'K'; resBuffer[3] = '\\'; resBuffer[4] = 'n';
					HAL_UART_Transmit(&huart3, resBuffer, 5, 0xFFFF);
					update = 0;
				}

				else if(aRxBuffer[0] == 'B' && aRxBuffer[1] == 'W'){
					goBackDist((aRxBuffer[2] - '0') * 100 + (aRxBuffer[3] - '0') * 10 + (aRxBuffer[4] - '0') * 1);
					resBuffer[0] = 'A'; resBuffer[1] = 'C'; resBuffer[2] = 'K'; resBuffer[3] = '\\'; resBuffer[4] = 'n';
					HAL_UART_Transmit(&huart3, resBuffer, 5, 0xFFFF);
					update = 0;
				}

				else if(aRxBuffer[0] == 'B' && aRxBuffer[1] == 'R'){
					turnRight90Back( 90 );
					resBuffer[0] = 'A'; resBuffer[1] = 'C'; resBuffer[2] = 'K'; resBuffer[3] = '\\'; resBuffer[4] = 'n';
					HAL_UART_Transmit(&huart3, resBuffer, 5, 0xFFFF);
					update = 0;
				}

				else if(aRxBuffer[0] == 'B' && aRxBuffer[1] == 'L'){
					turnLeft90Back( 90 );
					resBuffer[0] = 'A'; resBuffer[1] = 'C'; resBuffer[2] = 'K'; resBuffer[3] = '\\'; resBuffer[4] = 'n';
					HAL_UART_Transmit(&huart3, resBuffer, 5, 0xFFFF);
					update = 0;
				}
				else if(aRxBuffer[0] == 'A' && aRxBuffer[1] == 'D'){
					adjustDist((aRxBuffer[2] - '0') * 100 + (aRxBuffer[3] - '0') * 10 + (aRxBuffer[4] - '0'));
					resBuffer[0] = 'A'; resBuffer[1] = 'C'; resBuffer[2] = 'K'; resBuffer[3] = '\\'; resBuffer[4] = 'n';
					update = 0;
					HAL_UART_Transmit(&huart3, resBuffer, 5, 0xFFFF);
				}

				else{
					resBuffer[0] = 'I'; resBuffer[1] = 'N'; resBuffer[2] = 'V'; resBuffer[3] = '\\'; resBuffer[4] = 'n';
					HAL_UART_Transmit(&huart3, resBuffer, 5, 0xFFFF);
					update = 0;
				}


	}
	osDelay(100);

  }
  /* USER CODE END 5 */
}

/* USER CODE BEGIN Header_show */
/**
* @brief Function implementing the showOled thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_show */
void show(void *argument)
{
  /* USER CODE BEGIN show */
  /* Infinite loop */
    OLED_Init();

	uint8_t hello[20] = "Hello World!\0";
  for(;;)
  {
	//
	OLED_Refresh_Gram();
  sprintf(hello, "Distance: %4d\0", Distance);
  OLED_ShowString(5, 10, hello);
	sprintf(hello, "Gyro:%5d\0", (int) total_angle);
	OLED_ShowString(5, 20, hello);
	sprintf(hello, "L-IR:%3d R-IR:%3d", IRLeft, IRRight );
	OLED_ShowString(5, 30, hello);
	//sprintf(hello, "ob1:%3d ow%3d", obDist1, obWidth2);
	OLED_ShowString(5, 40, hello);
	OLED_ShowString(5, 50, globalMessages);
    osDelay(150);
  }
  /* USER CODE END show */
}

/* USER CODE BEGIN Header_ultrasonicTask */
/**
* @brief Function implementing the Ultrasonic thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_ultrasonicTask */
void ultrasonicTask(void *argument)
{
  /* USER CODE BEGIN ultrasonicTask */
  /* Infinite loop */
	uint32_t adcVal1, adcVal2;
	float volt1, volt2;
	 __HAL_TIM_ENABLE_IT(&htim4, TIM_IT_CC1);
	   HAL_TIM_IC_Start_IT(&htim4, TIM_CHANNEL_1);
  for(;;)
  {
	  HCSR04_Read();
    osDelay(80);


		//IR
	HAL_ADC_Start(&hadc1);
	HAL_ADC_PollForConversion(&hadc1, 100);
	adcVal1 = HAL_ADC_GetValue(&hadc1);
	volt1 = (adcVal1 / pow(2, 12)) * 3.3;
	IRRight = 1 /  (0.0140817 * pow(volt1, 2) + 0.00685361 * volt1 + 0.012403);

	HAL_ADC_Start(&hadc2);
	HAL_ADC_PollForConversion(&hadc2, 100);
	adcVal2 = HAL_ADC_GetValue(&hadc2);
	volt2 = (adcVal2 / pow(2, 12)) * 3.3;
	IRLeft = 1 /  (0.0140817 * pow(volt2, 2) + 0.00685361 * volt2 + 0.012403);
  }
  /* USER CODE END ultrasonicTask */
}

/* USER CODE BEGIN Header_GyroTask */
/**
* @brief Function implementing the gyro thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_GyroTask */
void GyroTask(void *argument)
{
  /* USER CODE BEGIN GyroTask */
		gyroInit();

		uint8_t val[2] = {0,0};
		int16_t angular_speed = 0;
		uint32_t tick = 0;
		double offset = 0;
		double trash= 0;
		volatile int ticker=0;
		osDelay(300);
		while(ticker<100){
			osDelay(50);
			readByte(0x37, val);
			angular_speed = (val[0] << 8) | val[1];
			trash +=(double)((double)angular_speed)*((HAL_GetTick() - tick)/16400.0);
			tick = HAL_GetTick();
			offset += angular_speed;
			ticker++;
		}
		//buzzerBeep(100);
		offset = offset/(ticker);
		tick = HAL_GetTick();
		notdone=0;


    /* Infinite loop */
  for(;;)
  {
			//Gyro
			readByte(0x37, val);
			angular_speed = (val[0] << 8) | val[1];
			total_angle +=(double)((double)angular_speed - offset)*((HAL_GetTick() - tick)/16400.0);
			tick = HAL_GetTick();
			ticker -= angular_speed;
			ticker++;




			osDelay(40);

  }
  /* USER CODE END GyroTask */
}

/* USER CODE BEGIN Header_IRTask */
/**
* @brief Function implementing the IRtask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_IRTask */
void IRTask(void *argument)
{
  /* USER CODE BEGIN IRTask */
  /* Infinite loop */
  uint32_t adcVal1, adcVal2;
  float volt1, volt2;
  for(;;)
  {
		//IR
	HAL_ADC_Start(&hadc1);
	HAL_ADC_PollForConversion(&hadc1, 100);
	adcVal1 = HAL_ADC_GetValue(&hadc1);
	volt1 = (adcVal1 / pow(2, 12)) * 3.3;
	IRRight = 1 /  (0.0140817 * pow(volt1, 2) + 0.00685361 * volt1 + 0.012403);

	HAL_ADC_Start(&hadc2);
	HAL_ADC_PollForConversion(&hadc2, 100);
	adcVal2 = HAL_ADC_GetValue(&hadc2);
	volt2 = (adcVal2 / pow(2, 12)) * 3.3;
	IRLeft = 1 /  (0.0140817 * pow(volt2, 2) + 0.00685361 * volt2 + 0.012403);

	osDelay(100);
  }
  /* USER CODE END IRTask */
}

/**
  * @brief  This function is executed in case of error occurrence.
  * @retval None
  */
void Error_Handler(void)
{
  /* USER CODE BEGIN Error_Handler_Debug */
  /* User can add his own implementation to report the HAL error return state */
  __disable_irq();
  while (1)
  {
  }
  /* USER CODE END Error_Handler_Debug */
}

#ifdef  USE_FULL_ASSERT
/**
  * @brief  Reports the name of the source file and the source line number
  *         where the assert_param error has occurred.
  * @param  file: pointer to the source file name
  * @param  line: assert_param error line source number
  * @retval None
  */
void assert_failed(uint8_t *file, uint32_t line)
{
  /* USER CODE BEGIN 6 */
  /* User can add his own implementation to report the file name and line number,
     ex: printf("Wrong parameters value: file %s on line %d\r\n", file, line) */
  /* USER CODE END 6 */
}
#endif /* USE_FULL_ASSERT */
