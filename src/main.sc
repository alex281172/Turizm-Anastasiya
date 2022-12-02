require: patterns.sc

require: funcs.js

require: slotfilling/slotFilling.sc
    module = sys.zb-common
    
require: city/city.sc
    module = sys.zb-common

init:
    bind("postProcess", function($context) {
        $context.session.lastState = $context.currentState;
    });
    
theme: /

    state: Start
        q!: *start
        q!: * $hello *
        q: * (отмен*/стоп) * || fromState = /SuggestMenu
        script:
            $temp.botName = capitalize($injector.botName);
        random:
            a: Здравствуйте! Меня зовут {{$temp.botName}}, я бот туристической компании “Just Tour”. Готов проконсультировать вас о погоде в городах и странах мира и помочь оформить заявку на подбор тура.
            a: Приветствую! Я бот турагенства "Just Tour" по имени {{$temp.botNameme}}. Расскажу прогноз погоды в любой точке мира или оформлю заявку, чтобы менеджер турагенства помог с выбором тура.
            a: Доброе день! Я {{$temp.botName}}, бот турагенства "Just Tour". Могу дать прогноз погоды, где пожелаете, или помочь оформить заявку на подбор тура с менеджером.
        script:
            $response.replies = $response.replies || [];
            $response.replies.push({
                type: "image",
                imageUrl: "https://traveltimes.ru/wp-content/uploads/2021/07/problemy-turistov.jpg",
                text: "Погода и путешествия"
            });
        go!: /SuggestMenu
    
    state: CatchAll || noContext = true
        event!: noMatch
        random:
            a: Простите, я вас не понял. Попробуйте ответить по-другому.
            a: Извините, я не понимаю. Переформулируйте, пожалуйста, свой запрос.
        go!: {{$session.lastState}}
            
    state: SuggestMenu || modal = true
        random:
            a: Хотите узнать погоду или оформить заявку?
            a: Прогноз погоды или оформим заявку?
            a: Что выбираете из этих двух опций?
        buttons:
                "Прогноз погоды"
                "Оформить заявку"
            
        state: ChooseWeather
            q: * ($weather) *
            go!: /GetDate
        
        state: ChooseRequest    
            q: * ($request) *
            go!: /ChooseDestination
            
        state: LocalCatchAll
            event: noMatch
            a: Не совсем вас понял. Ответьте, пожалуйста, еще раз на вопрос.
            go!: ..
    
    state: GetDate
        a: Назовите дату, на которую хотите узнать прогноз погоды.
        
    state: DateDefine
        intent!: /geo
        script:
            if ($parseTree._Date) {
            $temp.date = $parseTree._Date.value;
            } else {
            $temp.date = "сегодня";
            }
        
    state: GetWeather
        intent!: /geo
        script:
            var city = $parseTree._geo;
            openWeatherMapCurrent("metric", "ru", city).then(function (res) {
                if (res && res.weather) {
                    $reactions.answer("На выбранную дату в " + capitalize($nlp.inflect(city, "loc2")) + " " + res.weather[0].description + ", " + Math.round(res.main.temp) + "°C" );
                    if(Math.round(res.main.temp) > 20 ) {
                        $reactions.answer("Вы дествительно планируете поехать в страну с жарким климатом?")
                    } else if (Math.round(res.main.temp) < 0) {
                        $reactions.answer("Вы действительно планируете поездку в страну с холодным климатом?")
                    } else if (Math.round(res.main.temp) > 0 || Math.round(res.main.temp) <= 20 ) {
                        $reactions.answer("Вы действительно планируете поездку в страну с умеренным климатом?")
                    }
                } else {
                    $reactions.answer("Что-то сервер барахлит. Не могу узнать погоду.");
                }
            }).catch(function (err) {
                $reactions.answer("Что-то сервер барахлит. Не могу узнать погоду.");
            }); 
                
        state: ClientAgree
            q: * ($agree) *
            a: Хотели бы оформить тур в эту страну?
                
            state: Yes
                q: * ($agree) *
                go!: /ChooseDestination/HowManyAdults
                
            state: No
                q: * ($disagree) *
                a: Продолжим оформление уже начатой заявки?
                    
                state: Yes
                    q: * ($agree) *
                    go!: /ChooseDestination/HowManyAdults
                
                state: No
                    q: * ($disagree) *
                    a: Хотите узнать о климате в другом городе или стране мира?
                   
                    state: Yes
                        q: * ($agree) *
                        go!: /GetDate
                    
                    state: No
                        q: * ($disagree) *
                        go!: /Bye
                   
        state: ClientDisagree
            q: * ($disagree) *
            a: Хотите узнать погоду в другом городе или стране?
                
            state: Yes
                q: * ($agree) *
                go!: /GetDate
                    
            state: No
                q: * ($disagree) *
                go!: /Bye
                
    state: ChooseDestination
        a: В какую страну или город хотите поехать?

        state: ChooseTour
            q: * @mystem.geo *
            go!: /ChooseDestination/HowManyAdults
            
        state: NoIdea
            intent: /noidea
            a: С подбором тура вам поможет менеджер. Заполним заявку?
            
            state: ClientDisagree
                q: * ($disagree) *
                go!: /Bye
            
            state: ClientAgree
                q: * ($agree) *
                go!: /ChooseDestination/HowManyAdults
                
        state: HowManyAdults
            a: Сколько поедет взрослых?
            
            state: HowManyAdultsClient
                q: * @duckling.number *
                go!: /ChooseDestination/HowManyKids
            
        state: HowManyKids
            a: Сколько детей в поездке?
            
            state: HowManyKidsClient
                q: * @duckling.number *
                go!: /ChooseDestination/Budget
            
        state: Budget
            a: Какой бюджет путешествия? Добавьте к ответу вид валюты (RUB,€ и т.д.). 
            
            state: BudgetClient
                q: * @duckling.amount-of-money *
                go!: /ChooseDestination/DateTour
            
        state: DateTour
            a: Дата начала поездки?
            
            state: DateTourClient
                q: * @duckling.date *
                go!: /ChooseDestination/Duration
            
        state: Duration
            a: Длительность тура? Например: 8 дней.
            
            state: DurationClient
                q: * @duckling.duration *
                go!: /ChooseDestination/HowManyStars
        
        state: HowManyStars
            a: Какова желаемая "звездность" отеля?
            
            state: HowManyStarsClient
                q: * @duckling.number *
                go!: /ChooseDestination/ClientName
        
        state: ClientName
            a: Ваше имя?
            
            state: ClientNameClient
                q: * @pymorphy.name *
                go!: /ChooseDestination/PhoneNumber
        
        state: PhoneNumber
            a: Номер телефона для связи?
            
            state: PhoneNumberClient
                q: * @duckling.phone-number *
                go!: /ChooseDestination/Comment
            
        state: Comment
            a: Комметарий для менеджера в свободной форме?
            
            state: CommentClient
                q: *
                go!: /ChooseDestination/Email
            
        state: Email
            Email: 
                destination = roadsworld@mail.ru
                subject = Данные клиента
                text = Имя, номер телефона {{$parseTree["_duckling.phone-number"]}}
                files = []
                html = Имя, номер телефона
                htmlEnabled = false
                okState = /ChooseDestination/EmailSuccess
                errorState = /ChooseDestination/EmailFail

        state: EmailFail
                a: При отправке заявки возникли неполадки. За подбором тура вам необходимо обратиться в компанию по телефону 8(812)000-00-00. || htmlEnabled = false, html = "При отправке заявки возникли неполадки. За подбором тура вам необходимо обратиться в компанию по телефону 8(812)000-00-00."
                go!: /Bye

        state: EmailSuccess
                a: Заявка успешно отправлена. В ближайшее время с вами свяжется менеджер компании. || htmlEnabled = false, html = "Заявка успешно отправлена. В ближайшее время с вами свяжется менеджер компании."
                go!: /Bye

    state: Bye
        a: Всего доброго! До свидания!