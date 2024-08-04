<?php

    header("Content-Type: application/json");
    require $_SERVER['DOCUMENT_ROOT'].'/scouter/incl/database.class.php';
    $db = new Database('username','password','database','127.0.0.1', 'null');
    $db->connect();
    $headers = apache_request_headers();
    $token = $headers['token'];

    if ($_SERVER['REQUEST_METHOD'] == 'POST') 
    {
        
        
        $data = json_decode(file_get_contents("php://input"), true);
        $userData = $data[0];
        
        $world = $userData['world'];
        $health = $userData['health'];
        $time = $userData['time'];
        $timer= $userData['timer'];
        
        if (is_numeric($world)) {
            if (is_numeric($health)) {
                if (is_numeric($time)) {
                    if (is_numeric($timer)) {
                        
             
                        switch ($world) 
                        {
                            case 307:
                            case 309:
                            case 311:
                            case 389:
                               $current = $db->getDataForWorld($userData['world']);
                                if ($current['timestamp'] <  $time && (( $time + 10) < time() + 20)) 
                                {   
                                    
                                    $db->updateWorldData($world,  $health, $timer,  $time);
                                    echo 'success';
                                }
                                break;
                        }
        
                    }
                }
            }
        }
    } 

    if ($_SERVER['REQUEST_METHOD'] == 'GET') 
    {
        if ($_SERVER['HTTP_AUTHORIZATION'] == "2") {
            $results = $db->getAllData2();
            echo(json_encode($results));
        } else {
            $results = $db->getAllData();
            echo(json_encode($results));
        }
    }
?>
