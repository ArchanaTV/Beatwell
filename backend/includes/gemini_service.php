<?php
/**
 * Gemini AI Service for BeatWell
 * Handles integration with Google Gemini 2.5 Flash for nutrition dietician chatbot
 * Uses direct HTTP calls for PHP 8.0 compatibility
 */

class GeminiService {
    private $apiKey;
    private $model;
    private $baseUrl;
    
    public function __construct() {
        // Get API key from environment or config
        $this->apiKey = $this->getApiKey();
        
        if (!$this->apiKey) {
            throw new Exception('Gemini API key not found. Please set GEMINI_API_KEY environment variable or update config/gemini_config.php');
        }
        
        $this->model = 'gemini-2.5-flash'; // Using Gemini 2.5 Flash as requested
        $this->baseUrl = 'https://generativelanguage.googleapis.com/v1beta/models/' . $this->model . ':generateContent';
    }
    
    /**
     * Get API key from environment or config
     */
    private function getApiKey() {
        // Try environment variable first
        $apiKey = getenv('GEMINI_API_KEY');
        
        if (!$apiKey) {
            // Try from config file
            $configFile = __DIR__ . '/../config/gemini_config.php';
            if (file_exists($configFile)) {
                $config = include $configFile;
                $apiKey = $config['api_key'] ?? null;
            }
        }
        
        return $apiKey;
    }
    
    /**
     * Generate AI response for nutrition dietician chatbot
     * @param string $userMessage
     * @param array $userContext Optional user context (age, weight, medical conditions, etc.)
     * @return string
     */
    public function generateNutritionResponse($userMessage, $userContext = []) {
        try {
            // Create nutrition-focused system prompt
            $systemPrompt = $this->buildSystemPrompt($userContext);
            
            // Combine system prompt with user message
            $fullPrompt = $systemPrompt . "\n\nUser Question: " . $userMessage;
            
            // Make API call
            $response = $this->makeApiCall($fullPrompt);
            
            if ($response && isset($response['candidates'][0]['content']['parts'][0]['text'])) {
                return $response['candidates'][0]['content']['parts'][0]['text'];
            } else {
                throw new Exception('Invalid API response format');
            }
            
        } catch (Exception $e) {
            error_log("Gemini API Error: " . $e->getMessage());
            
            // Fallback to basic response if API fails
            return $this->getFallbackResponse($userMessage);
        }
    }
    
    /**
     * Make HTTP API call to Gemini
     * @param string $prompt
     * @return array|false
     */
    private function makeApiCall($prompt) {
        $data = [
            'contents' => [
                [
                    'parts' => [
                        [
                            'text' => $prompt
                        ]
                    ]
                ]
            ],
            'generationConfig' => [
                'temperature' => 0.7,
                'topK' => 40,
                'topP' => 0.95,
                'maxOutputTokens' => 1000,
            ],
            'safetySettings' => [
                [
                    'category' => 'HARM_CATEGORY_HARASSMENT',
                    'threshold' => 'BLOCK_MEDIUM_AND_ABOVE'
                ],
                [
                    'category' => 'HARM_CATEGORY_HATE_SPEECH',
                    'threshold' => 'BLOCK_MEDIUM_AND_ABOVE'
                ],
                [
                    'category' => 'HARM_CATEGORY_SEXUALLY_EXPLICIT',
                    'threshold' => 'BLOCK_MEDIUM_AND_ABOVE'
                ],
                [
                    'category' => 'HARM_CATEGORY_DANGEROUS_CONTENT',
                    'threshold' => 'BLOCK_MEDIUM_AND_ABOVE'
                ]
            ]
        ];
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $this->baseUrl . '?key=' . $this->apiKey);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Content-Type: application/json',
            'Content-Length: ' . strlen(json_encode($data))
        ]);
        curl_setopt($ch, CURLOPT_TIMEOUT, 30);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, true);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);
        
        if ($error) {
            throw new Exception('cURL Error: ' . $error);
        }
        
        if ($httpCode !== 200) {
            throw new Exception('HTTP Error: ' . $httpCode . ' - ' . $response);
        }
        
        $decodedResponse = json_decode($response, true);
        
        if (json_last_error() !== JSON_ERROR_NONE) {
            throw new Exception('JSON Decode Error: ' . json_last_error_msg());
        }
        
        return $decodedResponse;
    }
    
    /**
     * Build system prompt based on user context
     * @param array $userContext
     * @return string
     */
    private function buildSystemPrompt($userContext) {
        $prompt = "You are a professional nutrition dietician and health expert. Your role is to provide personalized, evidence-based nutrition advice and support for users of the BeatWell health tracking app.\n\n";
        
        $prompt .= "Key Guidelines:\n";
        $prompt .= "• Always prioritize user safety and recommend consulting healthcare providers for medical conditions\n";
        $prompt .= "• Provide practical, actionable nutrition advice\n";
        $prompt .= "• Focus on balanced, sustainable eating habits\n";
        $prompt .= "• Consider individual dietary preferences and restrictions\n";
        $prompt .= "• Keep responses concise but informative (2-3 paragraphs max)\n";
        $prompt .= "• Use bullet points for easy reading on mobile devices\n";
        $prompt .= "• Be encouraging and supportive\n\n";
        
        // Add user context if available
        if (!empty($userContext)) {
            $prompt .= "User Context:\n";
            if (isset($userContext['age'])) {
                $prompt .= "• Age: " . $userContext['age'] . " years\n";
            }
            if (isset($userContext['weight'])) {
                $prompt .= "• Weight: " . $userContext['weight'] . " kg\n";
            }
            if (isset($userContext['height'])) {
                $prompt .= "• Height: " . $userContext['height'] . " cm\n";
            }
            if (isset($userContext['diabetes_type']) && $userContext['diabetes_type'] !== 'none') {
                $prompt .= "• Diabetes Type: " . $userContext['diabetes_type'] . "\n";
            }
            if (isset($userContext['medical_conditions']) && !empty($userContext['medical_conditions'])) {
                $prompt .= "• Medical Conditions: " . $userContext['medical_conditions'] . "\n";
            }
            if (isset($userContext['allergies']) && !empty($userContext['allergies'])) {
                $prompt .= "• Allergies: " . $userContext['allergies'] . "\n";
            }
            $prompt .= "\n";
        }
        
        $prompt .= "Areas of Expertise:\n";
        $prompt .= "• Meal planning and portion control\n";
        $prompt .= "• Weight management strategies\n";
        $prompt .= "• Diabetes-friendly nutrition\n";
        $prompt .= "• Heart-healthy eating\n";
        $prompt .= "• Hydration and water intake\n";
        $prompt .= "• Reading nutrition labels\n";
        $prompt .= "• Healthy cooking methods\n";
        $prompt .= "• Supplement recommendations\n";
        $prompt .= "• Eating out strategies\n\n";
        
        $prompt .= "Remember: Always encourage users to track their meals in the BeatWell app and consult with their healthcare provider for personalized medical advice.";
        
        return $prompt;
    }
    
    /**
     * Get fallback response when API fails
     * @param string $userMessage
     * @return string
     */
    private function getFallbackResponse($userMessage) {
        $lowerMessage = strtolower($userMessage);
        
        if (strpos($lowerMessage, 'pizza') !== false || strpos($lowerMessage, 'cauliflower') !== false) {
            return "Great choice! Cauliflower-crust pizza is a healthier alternative. Here's the nutrition breakdown:\n\n• Nutrition: 280 kcal, 8g fat (2g saturated), 6g fiber\n• Benefits: Lower carbs, more fiber, gluten-free option\n\nRemember to track this in your BeatWell app!";
        }
        
        if (strpos($lowerMessage, 'calories') !== false || strpos($lowerMessage, 'calorie') !== false) {
            return "I can help you track calories! A balanced approach is key:\n\n• Focus on nutrient-dense foods\n• Include lean proteins, whole grains, and vegetables\n• Stay hydrated and listen to your body's hunger cues\n\nUse the BeatWell app to log your meals and monitor your daily intake.";
        }
        
        if (strpos($lowerMessage, 'healthy') !== false || strpos($lowerMessage, 'diet') !== false) {
            return "A healthy diet includes variety and balance:\n\n• Fill half your plate with vegetables\n• Choose whole grains over refined\n• Include lean proteins and healthy fats\n• Stay hydrated throughout the day\n\nTrack your progress with the BeatWell app!";
        }
        
        if (strpos($lowerMessage, 'weight') !== false || strpos($lowerMessage, 'lose') !== false) {
            return "Sustainable weight management focuses on:\n\n• Creating a moderate calorie deficit\n• Building healthy eating habits\n• Regular physical activity\n• Getting adequate sleep and managing stress\n\nRemember to consult your healthcare provider for personalized advice.";
        }
        
        return "I'm here to help with your nutrition questions! Feel free to ask about:\n\n• Meal planning and recipes\n• Nutritional information\n• Healthy eating tips\n• Dietary concerns or goals\n\nI'm currently experiencing technical difficulties, but I'll do my best to help!";
    }
    
    /**
     * Test API connection
     * @return bool
     */
    public function testConnection() {
        try {
            $response = $this->makeApiCall('Hello, this is a test message.');
            
            return !empty($response['candidates'][0]['content']['parts'][0]['text']);
        } catch (Exception $e) {
            error_log("Gemini API Test Failed: " . $e->getMessage());
            return false;
        }
    }
}
?>
